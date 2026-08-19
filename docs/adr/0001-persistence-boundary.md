# ADR 0001: Persistence boundary for NeonGrid progression

## Status

Proposed

## Context

NeonGrid currently has several Room database classes and repository abstractions that overlap in responsibility. `GameDatabase` stores the active player save state and loadouts, while `AppDatabase` and `PlayerCharacterDatabase` expose broader character, inventory, skill, and status-effect entities. This was useful during prototyping, but it makes it unclear which database is the source of truth for player progression.

The first stabilization phase should define a single persistence boundary before adding more missions, equipment, skills, and meta-progression.

## Decision

Use `AppDatabase` as the long-term canonical database for player profile and progression data:

- active save slot state;
- character stats and attributes;
- inventory and equipped items;
- unlocked skills and skill definitions;
- status effects;
- custom loadouts.

Keep `GameDatabase` temporarily as a compatibility layer for the existing `GameViewModel` path until save/load mapping is extracted and callers can be migrated. Do not add new gameplay persistence features to `GameDatabase` unless they are part of that migration.

Runtime gameplay classes such as `Player`, `Enemy`, `Quest`, `Inventory`, `EquipmentItem`, and `SkillNode` remain domain models. Room entities remain storage models. Conversions between both sides should move into explicit mapper classes instead of being embedded in view models.

## Consequences

- New persistence work has a clear home: `AppDatabase` plus `DataRepository` or a successor repository built around the same boundary.
- `GameViewModel` can be simplified once save/load mapping is moved out.
- Existing destructive migrations should be replaced with explicit Room migrations before release builds rely on real player data.
- Tests should verify round trips from runtime state to persisted state and back.

## Follow-up tasks

1. Add `PlayerSaveStateMapper` with focused unit tests.
2. Migrate `GameViewModel` save/load code to the mapper.
3. Add migration tests before changing database versions again.
4. Remove or deprecate redundant database/repository paths after callers are migrated.
