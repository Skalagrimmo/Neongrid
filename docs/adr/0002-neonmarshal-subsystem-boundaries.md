# ADR 0002: NeonMarshal subsystem boundaries

## Status

Proposed

## Context

NeonGrid already contains the Android-facing RPG, mission, progression, and persistence runtime.
NanoMarshal contains the tactical runtime: voxel terrain, destructible cover, combat, tactical AI, behavior trees, hazards, pathfinding, and the 60 FPS game tick.

The stable external tools are deliberately implemented in separate runtimes:

- PixelGen 1.0 — objective semantic world generation and dynamic world state.
- EBE 1.0 — subjective observation, memory, belief, knowledge, communication, reaction, and semantic action requests.
- FPE 1.0 — deterministic read-only world projection/view state.

The integration must preserve the authority boundaries already frozen by those tools.

## Decision

NeonMarshal will use the following ownership model:

```text
                         ┌──────────────────────┐
                         │      PixelGen        │
                         │ objective world truth│
                         └──────────┬───────────┘
                                    │
                        events + local observations
                                    │
                                    v
                         ┌──────────────────────┐
                         │        EBE           │
                         │ subjective cognition│
                         └──────────┬───────────┘
                                    │
                         semantic action request
                                    │
                                    v
                         ┌──────────────────────┐
                         │    NanoMarshal       │
                         │ tactical simulation  │
                         └──────────┬───────────┘
                                    │
                         gameplay state / results
                                    │
                                    v
                         ┌──────────────────────┐
                         │      Neongrid        │
                         │ RPG/meta/persistence │
                         └──────────────────────┘

                         FPE
                          │
                          └── read-only deterministic projection
```

### Rules

1. PixelGen remains the only authority for objective generated-world semantics.
2. EBE never becomes a second source of world truth.
3. EBE observations are not automatically knowledge; locality and provenance are preserved.
4. A semantic action request is intent, not authority. World mutation happens through the owning runtime.
5. NanoMarshal owns moment-to-moment tactical simulation: movement, combat, cover, AI, hazards, and tactical state.
6. Neongrid owns player-facing campaign state, progression, persistence, missions, inventory, and Android UI orchestration.
7. FPE is tooling/projection only and never mutates the canonical world.
8. Android UI must not directly depend on PixelGen/EBE/FPE implementation internals.

## Stable dependency baseline

| Component | Baseline |
| --- | --- |
| NanoMarshal | GitHub submodule `neonmarshal-core`, pinned at `62ee54b` |
| Neongrid | foundation branch `neonmarshal-foundation`, isolated from `main` |
| PixelGen | `1.0.0 stable` |
| EBE | `1.0.0 stable` |
| FPE | `1.0.0 stable` |

The stable package contracts are frozen externally. Integration code should depend on explicit adapters/contracts rather than copying their internal implementation into Android classes.

## First vertical slice

The first NeonMarshal slice will prove this loop:

```text
load campaign
  -> obtain semantic sector from PixelGen
  -> expose local evidence to EBE
  -> enter NanoMarshal tactical session
  -> resolve tactical event
  -> emit semantic result/event
  -> update campaign/progression state
  -> persist
  -> restore consistently
```

Narrative decoration and broad content generation are explicitly deferred until this loop is stable.

## Consequences

- Existing Neongrid UI remains untouched during the foundation stage.
- NanoMarshal can be integrated incrementally instead of replacing the current GameViewModel in one step.
- PixelGen/EBE/FPE can remain outside the Android runtime process initially and communicate through versioned serialized contracts.
- Future in-process integration can replace adapters without changing ownership semantics.


## Current adapter layer

The foundation branch now exposes narrow Android-side boundaries:

| Boundary | Android adapter | Role |
| --- | --- | --- |
| PixelGen authority | `pixelgen/PixelGenFrozenBundleAdapter` | Read-only semantic world + ordered derived events |
| EBE runtime | `ebe/EbeRuntimeBundleAdapter` | Import/replay EBE observations and action requests |
| NanoMarshal | `nanomarshal/NanoMarshalRuntimeAdapter` | Lifecycle boundary around an injected tactical facade |
| FPE | `fpe/FpeProjectionAdapter` | Read-only projection identity/integrity boundary |

The NanoMarshal and FPE adapters intentionally use injected facades. This keeps the Android module independent from implementation-specific runtime types and allows the external stable runtimes to remain separate until an explicit binding is introduced.

The EBE adapter is currently a bundle/snapshot adapter, not a full in-process cognition engine. It does not synthesize knowledge or expand locality; those semantics remain owned by EBE.

The PixelGen adapter is currently a frozen-bundle authority adapter. Before production use, its assumed public-bundle artifact names should be reconciled against an actual `write_public_bundle` output.

## Concrete NanoMarshal binding

NanoMarshal is now consumed as a Git submodule at `third_party/NanoMarshal`, pinned to the `neonmarshal-core` branch. Its extracted `:nanomarshal-core` Android library is included directly in the Neongrid Gradle build.

The concrete bridge is:

```text
NanoMarshal GameEngine
        │
        v
GameEngineNanoMarshalEngine
        │
        v
NanoMarshalGameEngineFacade
        │
        v
NanoMarshalRuntimeAdapter
        │
        v
NeonMarshalCoordinator
```

`NanoMarshalMissionResolver` maps a semantic world reference to an explicit tactical `Mission`; the resolver remains injected so PixelGen world identifiers do not become hard-coded inside NanoMarshal.

The facade translates only session lifecycle and terminal state back into Neongrid's neutral contract. It does not make NanoMarshal a second semantic-world authority, and it does not write world truth back into PixelGen.

Semantic world-event intake is now a real in-process boundary: Coordinator-delivered PixelGen events are validated by the facade and accepted by NanoMarshal's tactical core through a bounded event log. `SemanticTacticalEventHandlerRegistry` then interprets the supported `front_*` and `territory_*` events into NanoMarshal-owned derived state (`frontPressure`, `territoryControl`, and `alertLevel`). Unknown kinds remain logged but have no tactical interpretation. Duplicate event IDs are idempotent, and event revisions are monotonic; none of this grants NanoMarshal write access to PixelGen.

## Validation status

The adapter layer has JVM unit coverage for:

- coordinator ordering and mutation-domain routing;
- PixelGen bundle identity/event ordering;
- EBE snapshot observation assignment and action-request import;
- NanoMarshal facade lifecycle and session identity;
- FPE projection world identity/revision/fingerprint checks.

The current validation gates for this binding are passing:

- `:nanomarshal-core:compileDebugKotlin` — successful in the standalone NanoMarshal branch.
- `:app:compileDebugKotlin` — successful after the library extraction and Neongrid submodule integration.
- `:app:compileDebugUnitTestKotlin` — successful after repairing three stale `CombatSystemTest` fixtures.
- `NanoMarshalGameEngineFacadeTest` — targeted JVM test task successful.
- `SemanticTacticalEventHandlerRegistryTest` — supported event mapping, clamping, unknown-event behavior, and registry coverage.

A full Gradle/Android test run remains a broader environment-level gate and is not represented as passing by this ADR.
