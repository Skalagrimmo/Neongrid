# NeonMarshal world reference extraction spec

This document converts the preserved Neongrid scene plates into a practical asset backlog without changing runtime code yet.

## Extraction groups

| Group | Purpose | Runtime target |
|---|---|---|
| Ground | Streets, platforms, floors | Tile/ground atlas |
| Vertical | Walls, ledges, facades | Z-aware environment primitives |
| Connectors | Ladders, stairs, ramps, bridges | Interactive world elements |
| Architecture | Windows, doors, panels, structural modules | Prop/facade atlas |
| Street | Barriers, crates, terminals, lamps, signs | Prop atlas |
| Neon | Signs, strips, emissive panels | Decal/emissive atlas |
| Corruption | Organic growth, damage, contamination | Overlay/decal variants |
| Lighting | Local neon pools, warning zones, dark pockets | Renderer lighting presets |

## Asset rules

1. Prefer reusable pieces over complete scene plates.
2. Keep transparent pixel-art assets separate from opaque reference plates.
3. Preserve a consistent logical footprint so an asset can be attached to grid coordinates.
4. Record the intended Z behavior for every vertical asset.
5. Do not encode gameplay state in artwork.
6. Keep faction-specific or named-character assets out of the generic environment pack.
7. Use the existing low-spec renderer path as the compatibility floor.

## Required metadata for an extracted asset

Each runtime-ready environment asset should eventually expose:

- stable asset id;
- source reference id;
- logical width/height in grid units;
- anchor point;
- supported Z behavior;
- collision/occupancy category;
- whether it blocks sight;
- whether it is interactive;
- whether it emits light;
- optional damaged/corrupted variant.

## Priority

### P0
- floor and wall language;
- Z-level edge/ledge language;
- ladder/stair/bridge connectors;
- basic street props.

### P1
- facade modules;
- neon signage;
- terminals and industrial props;
- corruption/overgrowth overlays.

### P2
- faction/environmental variants;
- special set pieces;
- cinematic background elements.

The world reference plates remain unchanged and are never modified during extraction. Derived assets should be added as new files so the original visual source remains reproducible.
