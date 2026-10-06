# ADR-0008: NeonMarshal vertical-isometric world composition

- Status: Accepted
- Date: 2026-10-06
- Scope: rendering, level representation, environment assets
- Branch: neonmarshal-foundation

## Context

The original Neongrid-generated world references establish the intended meaning of a multi-level isometric environment. A level is not a flat floor with decorative height offsets. It is a set of spatial layers that can be simultaneously readable and connected.

The four preserved world reference plates are treated as visual references for this contract:

- `world_reference_01`: clean multi-level neon city block;
- `world_reference_02`: explicit vertical/cutaway district composition;
- `world_reference_03`: corrupted/overgrown variation;
- `world_reference_04`: denser environmental variation.

The current NeonMarshal renderer already has the required conceptual primitives: `Point3D` positions, Z-aware entities, `GameLevelMap`, `currentZLevel`, an isometric projection and vertical `zHeightOffset`.

## Decision

### 1. Z is a gameplay-space coordinate

Z is not a purely visual elevation value. A change in Z represents a distinct traversable spatial layer.

Every future environment feature should explicitly declare whether it:

- occupies one Z layer;
- spans multiple Z layers;
- connects two or more Z layers;
- merely decorates a layer without changing traversal.

### 2. Layers remain independently addressable

Environment assets must not require flattening the whole scene into one background image.

The runtime representation should remain compatible with:

`(x, y, z) -> isometric screen position`

and preserve per-layer interaction, occupancy, navigation and visibility.

### 3. Vertical connectors are first-class environment elements

Ladders, stairs, ramps, bridges, shafts and equivalent transitions should be represented as explicit world elements rather than encoded only in textures.

A connector has at minimum:

- source coordinate;
- destination coordinate;
- source Z;
- destination Z;
- traversal/interaction type.

### 4. Occlusion is part of the world composition

A higher or nearer layer may visually obscure a lower layer while the lower layer remains part of the simulation.

Environment rendering therefore must support controlled visibility/occlusion rather than solving the scene by baking everything into one bitmap.

### 5. Reference plates are source material, not runtime backgrounds

The 1408x768 world references stay under `assets/source/world/`.

They are used to derive reusable:

- floor/wall motifs;
- facade modules;
- signage;
- street furniture;
- Z-level transition pieces;
- decals;
- corruption/overgrowth variants;
- lighting and palette presets.

They should not become the primary runtime representation of the level.

## Consequences

This keeps the visual direction from the original Neongrid work while preserving the current NeonMarshal simulation architecture.

It also means future environment asset extraction can happen independently from gameplay logic: an asset can acquire a visual representation without changing AI, combat or persistence.

The important invariant is:

> **A world layer is real simulation space first and an isometric visual layer second.**

## Deferred work

The following are intentionally not implemented in this ADR:

- final tile atlas layout;
- exact wall/facade sprite dimensions;
- final Z-level visibility rules;
- special NPC visual identities;
- faction headquarters and vendor designs.

Those can be added after the current enemy/civilian presentation pipeline and environment renderer are verified on-device.
