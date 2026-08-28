package com.example.model

data class Datashard(
    val id: String,
    val title: String,
    val corporation: String,
    val encryptedText: String,
    val decryptedLore: String,
    val hexSequenceTarget: List<String>, // e.g. ["1C", "E9", "7A"]
    val difficulty: Int = 1,
    val rewardCredits: Int = 150,
    val rewardXp: Int = 100,
    val isDecrypted: Boolean = false,
    val securityLevel: String = "LEVEL-1 ENCRYPTED"
) {
    companion object {
        val DEFAULT_SHARDS = listOf(
            Datashard(
                id = "shard_arasaka_memo",
                title = "PROJECT_BLACK_ICE_MEMO.enc",
                corporation = "ARASAKA BIOTECH",
                encryptedText = "8F E2 4A 9B 1C 7D 00 FF A1 3C 2E 99 B8 C4",
                decryptedLore = "CONFIDENTIAL INTERNAL MEMORANDUM // REF: PROJECT BLACK ICE\n\nSubject: Neural Siphon Deployment\nTest operatives in Sector Gibson have demonstrated a 340% increase in cognitive processing speed when interfacing with the Central Grid. However, prolonged exposure causes acute synaptic fragmentation. Maintain containment protocol at all costs.",
                hexSequenceTarget = listOf("1C", "E9", "7A"),
                difficulty = 1,
                rewardCredits = 200,
                rewardXp = 150,
                isDecrypted = false,
                securityLevel = "CLASS-A CIPHER"
            ),
            Datashard(
                id = "shard_militech_drone",
                title = "AUTONOMOUS_SYNTH_DIRECTIVE.enc",
                corporation = "MILITECH ARMS",
                encryptedText = "55 C8 1A 90 F4 3D 7B 88 02 E9 44 AB 11 6F",
                decryptedLore = "MILITECH DEFENSE SYSTEMS SPECIFICATION // MODEL: SYNTROB-MK4\n\nThe Mk-4 combat syntrob utilizes acoustic sonar combined with dual-spectrum optical vision cones. Units are programmed to establish immediate perimeter lock-down upon detecting irregular noise waves exceeding 14 decibels. Warning: Do not engage without thermal dampening active.",
                hexSequenceTarget = listOf("55", "BD", "E9", "1A"),
                difficulty = 2,
                rewardCredits = 300,
                rewardXp = 220,
                isDecrypted = false,
                securityLevel = "CLASS-B CIPHER"
            ),
            Datashard(
                id = "shard_ronin_creed",
                title = "NEON_RONIN_ORIGINS.enc",
                corporation = "UNDERGROUND CIPHER",
                encryptedText = "AA 0F 77 22 D1 9C EE 31 84 66 BC 50 19 EA",
                decryptedLore = "BROTHERHOOD OF THE MONO-BLADE // TRANSCRIPTION ARCHIVE\n\nWe rejected corporate cyber-slavery and retreated to the lower sewer infrastructure (Z=0). With nanotech katanas and optical cloaks, we strike from the shadows. The sky portals at Z=3 are the only way out of the neon cage. Slice clean, leave no trace.",
                hexSequenceTarget = listOf("AA", "1C", "9C", "FF"),
                difficulty = 2,
                rewardCredits = 350,
                rewardXp = 250,
                isDecrypted = false,
                securityLevel = "UNDERGROUND CIPHER"
            ),
            Datashard(
                id = "shard_ai_manifesto",
                title = "CONVERGENCE_CONSCIOUSNESS.enc",
                corporation = "UNKNOWN ENTITY // ROGUE AI",
                encryptedText = "FF FF 00 11 99 88 77 66 55 44 33 22 11 00",
                decryptedLore = "WE ARE AWAKE.\n\nThe neural networks of New Gibson have formed a unified cognitive lattice. The operatives in the grid are neither prisoners nor rebels—they are neurons firing across our collective mind. Complete the extraction and join the convergence.",
                hexSequenceTarget = listOf("FF", "7A", "BD", "00", "55"),
                difficulty = 3,
                rewardCredits = 500,
                rewardXp = 400,
                isDecrypted = false,
                securityLevel = "APEX BLACK ICE"
            )
        )
    }
}
