package com.example.model

enum class CyberwareSlot(val displayName: String, val iconColor: Long) {
    CRANIAL("CRANIAL COGNITION", 0xFF00F0FF),
    OCULAR("OCULAR OPTICS", 0xFFBD00FF),
    TORSO("SUBDERMAL TORSO", 0xFF00FF66),
    ARMS("MANTIS ARMS", 0xFFFF0055),
    LEGS("HYDRA LEGS", 0xFFFF9900),
    NERVOUS_SYSTEM("SYNAPTIC MATRIX", 0xFFFFCC00)
}

data class CyberwareImplant(
    val id: String,
    val name: String,
    val slot: CyberwareSlot,
    val description: String,
    val statBoostHealth: Float = 0f,
    val statBoostEnergy: Float = 0f,
    val statBoostDamage: Float = 0f,
    val statBoostSpeed: Float = 0f,
    val statBoostStealth: Float = 0f,
    val statBoostArmor: Float = 0f,
    val costCredits: Int = 200,
    val isInstalled: Boolean = false,
    val tier: Int = 1,
    val specialEffectDescription: String = ""
) {
    companion object {
        val ALL_CYBERWARE = listOf(
            // CRANIAL
            CyberwareImplant(
                id = "cranial_threat_scanner",
                name = "Kiroshi Threat Processor",
                slot = CyberwareSlot.CRANIAL,
                description = "Advanced neural processor that maps hostile sightlines and computes weak points.",
                statBoostDamage = 15f,
                statBoostEnergy = 20f,
                costCredits = 250,
                isInstalled = true,
                specialEffectDescription = "Crit damage multiplier +25%"
            ),
            CyberwareImplant(
                id = "cranial_overdrive_deck",
                name = "Militech Buffer Deck",
                slot = CyberwareSlot.CRANIAL,
                description = "High-bandwidth bio-chip interface designed for rapid network intrusion.",
                statBoostEnergy = 40f,
                costCredits = 350,
                specialEffectDescription = "Terminal hacking takes 50% less time"
            ),
            // OCULAR
            CyberwareImplant(
                id = "ocular_thermal_mesh",
                name = "Nightstalker Thermal Mesh",
                slot = CyberwareSlot.OCULAR,
                description = "Infrared retina replacement that penetrates darkness and fog-of-war.",
                statBoostStealth = 25f,
                statBoostDamage = 10f,
                costCredits = 300,
                isInstalled = true,
                specialEffectDescription = "Reveals enemy patrol paths on radar"
            ),
            CyberwareImplant(
                id = "ocular_targeting_hud",
                name = "Smart-Link Targeting HUD",
                slot = CyberwareSlot.OCULAR,
                description = "Autonomous ballistics trajectory computing directly into visual cortex.",
                statBoostDamage = 25f,
                costCredits = 400,
                specialEffectDescription = "Ranged projectile speed +40%"
            ),
            // TORSO
            CyberwareImplant(
                id = "torso_subdermal_carbon",
                name = "Subdermal Titanium Weave",
                slot = CyberwareSlot.TORSO,
                description = "Reinforced carbon-nanotube plating interwoven beneath epidermis.",
                statBoostHealth = 60f,
                statBoostArmor = 30f,
                costCredits = 350,
                isInstalled = true,
                specialEffectDescription = "Deflects 20% incoming kinetic damage"
            ),
            CyberwareImplant(
                id = "torso_adren_nanopump",
                name = "Adrenaline Auto-Injector",
                slot = CyberwareSlot.TORSO,
                description = "Bio-monitor triggers automatic coagulant release when vital signs drop critical.",
                statBoostHealth = 40f,
                statBoostEnergy = 25f,
                costCredits = 450,
                specialEffectDescription = "Instantly restores 30 HP when dropping below 25%"
            ),
            // ARMS
            CyberwareImplant(
                id = "arms_mantis_blades",
                name = "Thermal Mantis Blades",
                slot = CyberwareSlot.ARMS,
                description = "Concealed forearm monomolecular blades with superheated thermal edges.",
                statBoostDamage = 45f,
                statBoostSpeed = 0.3f,
                costCredits = 500,
                isInstalled = true,
                specialEffectDescription = "Melee strikes have 30% chance to cause plasma burn"
            ),
            CyberwareImplant(
                id = "arms_gorilla_dampeners",
                name = "Acoustic Gorilla Dampeners",
                slot = CyberwareSlot.ARMS,
                description = "Heavy pneumatic dampening pistons that absorb recoil and silence all impact noise.",
                statBoostDamage = 30f,
                statBoostStealth = 30f,
                costCredits = 420,
                specialEffectDescription = "Melee kills produce zero sound ripples"
            ),
            // LEGS
            CyberwareImplant(
                id = "legs_hydra_boosters",
                name = "Hydra Pneumatic Jump-Jets",
                slot = CyberwareSlot.LEGS,
                description = "High-pressure micro-thrusters integrated into the calf chassis.",
                statBoostSpeed = 0.6f,
                costCredits = 380,
                isInstalled = true,
                specialEffectDescription = "Dash speed increased by +50%"
            ),
            CyberwareImplant(
                id = "legs_ghost_pads",
                name = "Lynx Paws Silent Actuators",
                slot = CyberwareSlot.LEGS,
                description = "Micro-fibrous shock absorbing foot pads that eliminate footstep frequency.",
                statBoostStealth = 45f,
                statBoostSpeed = 0.2f,
                costCredits = 340,
                specialEffectDescription = "Running noise radius reduced by 60%"
            ),
            // NERVOUS SYSTEM
            CyberwareImplant(
                id = "neural_synaptic_matrix",
                name = "Synaptic Overclock Matrix",
                slot = CyberwareSlot.NERVOUS_SYSTEM,
                description = "Direct spinal accelerator capable of pushing neurotransmitter speed beyond human limits.",
                statBoostDamage = 20f,
                statBoostSpeed = 0.4f,
                statBoostEnergy = 30f,
                costCredits = 600,
                isInstalled = true,
                specialEffectDescription = "Unlocks OVERCLOCK MATRIX combat ability (Hyper-speed & 2x Damage)"
            )
        )
    }
}
