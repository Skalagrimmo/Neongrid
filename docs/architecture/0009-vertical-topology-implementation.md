# Vertical topology implementation note

The first runtime-neutral topology primitive is now present in:

`app/src/main/java/com/example/model/VerticalWorldTopology.kt`

It intentionally does not modify `LevelManager` yet. The current level generator still encodes ladder tiles directly, so the next PC-side step is to construct the topology from those existing ladder pairs and then make movement/pathfinding consult the topology for Z transitions.

Current implementation provides:

- typed connector kinds;
- grid-aligned source/destination endpoints;
- bidirectional or one-way traversal;
- lookup by two grid positions;
- lookup of connectors originating at a grid position;
- participating-Z discovery;
- topology validation;
- unit coverage for forward/reverse lookup and invalid connectors.

This is the bridge between the existing tile-based level layout and the richer vertical-isometric world contract from ADR-0008.

## Next PC-side wiring

1. Instantiate `VerticalWorldTopology` from the existing `LevelManager` ladder pairs.
2. Replace implicit "same tile coordinate + Z delta" assumptions in movement with connector lookup.
3. Keep ladder visuals as the current representation initially.
4. Later allow the same connector to render as stairs, ramps, shafts or bridges without touching navigation.
