# NeonGrid RPG

NeonGrid RPG is an Android tactical stealth RPG prototype built with Kotlin and Jetpack Compose. The current gameplay direction is a cyberpunk isometric infiltration loop: move through multi-level sectors, manage stealth and noise, fight or bypass enemies, hack objectives, collect rewards, and grow a character build through gear and skills.

## Current feature slice

- Compose UI flow for the main menu, gameplay HUD, controls, loadout, skill tree, and native isometric canvas experiment.
- Runtime gameplay systems for movement, combat, stealth AI, level layout, enemy behavior, equipment, and skill progression.
- Room-backed local persistence for save state, character statistics, inventory, unlocked skills, status effects, and custom loadouts.
- GBC/cel-shaded visual settings and renderer classes for the isometric presentation.
- JVM/Robolectric tests for repositories, AI states, skill tree UI/viewmodel behavior, and stealth overlay rendering.

## Project layout

```text
app/src/main/java/com/example/
  data/       Room entities, DAOs, repositories, and save-state serialization
  engine/     Gameplay systems such as movement, combat, AI, levels, and skills
  model/      Runtime gameplay models for player, enemies, equipment, quests, and maps
  render/     Isometric and visual rendering helpers
  ui/         Jetpack Compose screens, HUDs, overlays, and view models
  viewmodel/  Additional character-facing view models
```

## Architecture direction

The project is still in prototype shape, so the first stabilization goal is to make the runtime state, persistence state, and UI state easier to reason about before adding a large amount of new content.

Near-term architecture priorities:

1. Keep gameplay formulas and state transitions testable outside Android UI classes.
2. Reduce `GameViewModel` toward orchestration only by extracting session, progression, save/load mapping, and navigation responsibilities.
3. Use one documented persistence boundary and avoid multiple competing repositories for the same gameplay data.
4. Move balancing data for equipment, skills, enemies, and objectives toward validated data files.
5. Prefer state containers that Compose can observe reliably when lists of enemies, noise ripples, projectiles, or explored tiles change.

See [`docs/adr/0001-persistence-boundary.md`](docs/adr/0001-persistence-boundary.md) for the initial persistence decision and [`PROJECT_ROADMAP_UK.md`](PROJECT_ROADMAP_UK.md) for a broader Ukrainian roadmap.

## Build and test

The repository currently does not include a Gradle wrapper, so use a locally installed Gradle distribution that is compatible with the configured Android Gradle Plugin and Kotlin versions.

Useful checks:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

If these fail during Gradle configuration, first verify the local JDK, Android SDK, Gradle, and Android Gradle Plugin compatibility.

## Recommended next milestone

Create one stable vertical slice: start a run, navigate a guarded route, hack a terminal, reach an extraction tile, receive a reward, save progress, and reload into a consistent state. That slice should become the baseline for future missions, AI variants, equipment, and roguelite modifiers.
