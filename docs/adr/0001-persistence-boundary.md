# ADR 0001: Persistence boundary for NeonGrid progression

## Status

Accepted (implemented in the Stage-1 data-hardening pass: entity-backed NeonMarshal results table, explicit 9 -> 10 migration, and narrow destructive-fallback windows in all three databases).

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
- Destructive migrations are no longer the default upgrade path. Each database now keeps data for current and adjacent versions: `AppDatabase` migrates 8 -> 9 -> 10 explicitly (`MIGRATION_8_9`, `MIGRATION_9_10`), while `GameDatabase` (v5) and `PlayerCharacterDatabase` (v4) rely on their entity schemas. Only pre-baseline versions without recorded migrations (AppDatabase 1..7, GameDatabase 1..4, PlayerCharacterDatabase 1..3) rebuild via `fallbackToDestructiveMigrationFrom(dropAllTables = true, ...)`; downgrades use `fallbackToDestructiveMigrationOnDowngrade(true)`. Schemas are still not exported (`exportSchema = false`), so schema-JSON-based `MigrationTestHelper` coverage remains a follow-up.
- Tests verify save-state serialization and mapper round trips from runtime state to persisted state and back, and `AppDatabaseMigrationTest` covers fresh-install table creation plus the 9 -> 10 repair of legacy databases.

## Follow-up tasks

1. Continue expanding `PlayerSaveStateMapper` coverage as new run-state fields are added.
2. Migrate remaining save/load callers to the canonical `AppDatabase` boundary.
3. Extend migration tests to `GameDatabase` and `PlayerCharacterDatabase` before changing their versions again; consider exporting schema JSONs (`exportSchema = true`) to enable `MigrationTestHelper`.
4. Remove or deprecate redundant database/repository paths after callers are migrated.
