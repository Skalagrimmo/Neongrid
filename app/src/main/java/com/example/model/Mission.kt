package com.example.model

enum class MissionType(val displayName: String, val badgeColor: Long) {
    DATA_HEIST("DATA HEIST", 0xFF00F0FF),
    SABOTAGE("GRID SABOTAGE", 0xFFFF9900),
    EXTRACTION("VIP EXTRACTION", 0xFF00FF66),
    ASSASSINATION("ELITE ELIMINATION", 0xFFFF3366)
}

enum class SectorHazard(val title: String, val description: String, val iconColor: Long) {
    NONE("CLEAR GRID", "Standard security protocols active.", 0xFF888888),
    EMP_SURGE("EMP STORM", "Energy regeneration reduced by 30%, but terminal hack speeds doubled.", 0xFF00E5FF),
    SENTRY_NET("DENSE SENTRY NET", "Enemy alert meters fill 25% faster.", 0xFFFF5555),
    LOW_LIGHT("BLACKOUT PROTOCOL", "Visibility range reduced, but player noise waves dissipate faster.", 0xFFBD00FF),
    HIGH_SECURITY("HEAVY LASER MATRIX", "All security gates have active backup defenses.", 0xFFFFCC00)
}

data class MissionContract(
    val id: String,
    val title: String,
    val codename: String,
    val type: MissionType,
    val description: String,
    val targetZLevel: Int = 3,
    val targetTerminals: Int = 2,
    val targetEnemies: Int = 3,
    val rewardCredits: Int = 250,
    val rewardXp: Int = 150,
    val hazard: SectorHazard = SectorHazard.NONE,
    val isCompleted: Boolean = false,
    var currentTerminalsHacked: Int = 0,
    var currentEnemiesEliminated: Int = 0,
    var isExtractionReady: Boolean = false
) {
    val progressPercent: Float
        get() {
            val totalObjectives = targetTerminals + targetEnemies
            if (totalObjectives == 0) return 1f
            val current = (currentTerminalsHacked.coerceAtMost(targetTerminals) +
                    currentEnemiesEliminated.coerceAtMost(targetEnemies))
            return (current.toFloat() / totalObjectives).coerceIn(0f, 1f)
        }

    val isObjectiveMet: Boolean
        get() = currentTerminalsHacked >= targetTerminals && currentEnemiesEliminated >= targetEnemies

    companion object {
        val DEFAULT_MISSIONS = listOf(
            MissionContract(
                id = "op_gibson_heist",
                title = "Operation Neuro-Siphon",
                codename = "GIBSON-HEIST",
                type = MissionType.DATA_HEIST,
                description = "Infiltrate the Arasaka sub-network, breach 2 central security uplinks, and reach the extraction conduit.",
                targetZLevel = 3,
                targetTerminals = 2,
                targetEnemies = 2,
                rewardCredits = 350,
                rewardXp = 200,
                hazard = SectorHazard.SENTRY_NET
            ),
            MissionContract(
                id = "op_blackout_substation",
                title = "Project Ghost-Grid",
                codename = "SABOTAGE-44",
                type = MissionType.SABOTAGE,
                description = "Overload the central plasma conduits and disable the sector's main power distribution matrix.",
                targetZLevel = 2,
                targetTerminals = 3,
                targetEnemies = 1,
                rewardCredits = 400,
                rewardXp = 250,
                hazard = SectorHazard.EMP_SURGE
            ),
            MissionContract(
                id = "op_zero_extract",
                title = "Extraction: Courier 7",
                codename = "VIP-EXTRACTION",
                type = MissionType.EXTRACTION,
                description = "Locate the lost neural data packet on the lower sewer levels and escape undetected through the rooftop gateway.",
                targetZLevel = 3,
                targetTerminals = 1,
                targetEnemies = 4,
                rewardCredits = 500,
                rewardXp = 300,
                hazard = SectorHazard.LOW_LIGHT
            ),
            MissionContract(
                id = "op_synth_warlord",
                title = "Execution: Synth-Lord Vane",
                codename = "CYBER-ASSASSIN",
                type = MissionType.ASSASSINATION,
                description = "Neutralize the elite combat syntrob commander patrolling the upper executive terrace.",
                targetZLevel = 3,
                targetTerminals = 1,
                targetEnemies = 5,
                rewardCredits = 650,
                rewardXp = 400,
                hazard = SectorHazard.HIGH_SECURITY
            )
        )
    }
}
