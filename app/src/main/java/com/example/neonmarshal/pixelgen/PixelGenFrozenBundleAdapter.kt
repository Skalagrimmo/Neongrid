package com.example.neonmarshal.pixelgen

import com.example.neonmarshal.bridge.SemanticWorldEvent
import com.example.neonmarshal.bridge.SemanticWorldRef
import com.example.neonmarshal.bridge.WorldAuthorityAdapter
import org.json.JSONArray
import org.json.JSONObject

/**
 * Read-only PixelGen 1.0/frozen-bundle adapter.
 *
 * PixelGen remains the objective-world authority. This adapter deliberately
 * consumes the serialized contract instead of reimplementing PixelGen in Kotlin.
 *
 * The adapter accepts the frozen bundle artifact names used by PixelGen:
 * world.json, state.json, events.json and pixelgen_contract_manifest.json.
 * The optional EBE artifact is not parsed here; that belongs to EBE's adapter.
 */
class PixelGenFrozenBundleAdapter(
    private val source: PixelGenBundleSource
) : WorldAuthorityAdapter {

    private val manifest by lazy {
        parseManifest(source.read("pixelgen_contract_manifest.json"))
    }

    private val stateArtifact by lazy {
        manifest.artifacts.firstOrNull { it.artifactType == "world_state" }
            ?: error("PixelGen bundle has no world_state artifact")
    }

    private val worldRef by lazy {
        require(manifest.relationships.worldSemanticFingerprint.isNotBlank()) {
            "PixelGen bundle has no semantic world fingerprint"
        }

        SemanticWorldRef(
            worldId = manifest.relationships.worldSemanticFingerprint,
            revision = stateArtifact.revision
                ?: error("PixelGen world_state artifact has no revision"),
            fingerprint = manifest.relationships.worldSemanticFingerprint
        )
    }

    override fun loadWorld(
        worldId: String,
        revision: Long?
    ): SemanticWorldRef {
        require(worldId == worldRef.worldId) {
            "Foreign PixelGen world: expected ${worldRef.worldId}, got $worldId"
        }

        if (revision != null) {
            require(revision <= worldRef.revision) {
                "Requested revision $revision is newer than bundle revision ${worldRef.revision}"
            }
        }

        return worldRef
    }

    override fun pollEvents(
        worldId: String,
        sinceRevision: Long?
    ): List<SemanticWorldEvent> {
        loadWorld(worldId, revision = sinceRevision)

        val state = JSONObject(source.read(stateArtifact.path))
        val derivedEvents = state.optJSONArray("derived_events") ?: JSONArray()
        val lowerBound = sinceRevision ?: 0L

        return buildList(derivedEvents.length()) {
            for (index in 0 until derivedEvents.length()) {
                val event = derivedEvents.getJSONObject(index)
                val revision = event.optLong("revision", index.toLong() + 1L)

                if (revision <= lowerBound) continue

                add(
                    SemanticWorldEvent(
                        eventId = event.getString("id"),
                        worldId = worldRef.worldId,
                        revision = revision,
                        kind = event.getString("event_type"),
                        payloadJson = event.toString()
                    )
                )
            }
        }.sortedBy { it.revision }
    }

    /**
     * The frozen bundle is intentionally read-only.
     *
     * PixelGen write-back will be introduced through a separate authenticated
     * authority transport rather than mutating a local Android copy.
     */
    override fun applyAction(
        request: SemanticActionRequest
    ): List<SemanticWorldEvent> {
        throw UnsupportedOperationException(
            "PixelGen frozen bundles are read-only; action ${request.requestId} requires a WorldMutationAdapter"
        )
    }

    private fun parseManifest(json: String): Manifest {
        val root = JSONObject(json)
        val artifactsJson = root.optJSONArray("artifacts") ?: JSONArray()
        val artifacts = buildList(artifactsJson.length()) {
            for (index in 0 until artifactsJson.length()) {
                val artifact = artifactsJson.getJSONObject(index)
                add(
                    Artifact(
                        artifactType = artifact.getString("artifact_type"),
                        path = artifact.optString(
                            "path",
                            defaultArtifactPath(artifact.getString("artifact_type"))
                        ),
                        revision = artifact.optLong("revision", -1L)
                            .takeIf { it >= 0L }
                    )
                )
            }
        }

        val relationships = root.optJSONObject("relationships")
            ?: error("PixelGen contract manifest has no relationships object")

        require(relationships.optBoolean("state_matches_world", true)) {
            "PixelGen manifest reports state/world identity mismatch"
        }
        require(relationships.optBoolean("events_match_state_log", true)) {
            "PixelGen manifest reports event/state mismatch"
        }

        return Manifest(
            manifestVersion = root.optString("manifest_version", "unknown"),
            artifacts = artifacts,
            relationships = Relationships(
                worldSemanticFingerprint = relationships.optString(
                    "world_semantic_fingerprint",
                    ""
                )
            )
        )
    }

    private fun defaultArtifactPath(artifactType: String): String = when (artifactType) {
        "world" -> "world.json"
        "world_state" -> "state.json"
        "runtime_events" -> "events.json"
        "ebe_runtime" -> "ebe-runtime.json"
        else -> error("Unknown PixelGen artifact type without path: $artifactType")
    }

    private data class Manifest(
        val manifestVersion: String,
        val artifacts: List<Artifact>,
        val relationships: Relationships
    )

    private data class Artifact(
        val artifactType: String,
        val path: String,
        val revision: Long? = null
    )

    private data class Relationships(
        val worldSemanticFingerprint: String
    )
}
