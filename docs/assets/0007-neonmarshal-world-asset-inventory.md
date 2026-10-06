# NeonMarshal world asset inventory

The original Neongrid Downloads folder contains four additional 1408x768 generated world images alongside the enemy and pedestrian sheets.

They have been copied into:

```
assets/source/world/
```

with stable names:

- `world_reference_01.png`
- `world_reference_02.png`
- `world_reference_03.png`
- `world_reference_04.png`

These are currently classified as **reference scene plates**, not runtime textures. The existing NeonMarshal renderer is grid/tile driven, so using an entire 1408x768 plate as a gameplay texture would bypass the current `GameLevelMap`, Z-level, lighting and interaction systems.

The useful next stage is extraction: identify reusable environmental motifs from these references and turn them into tiles, facades, props, decals, lighting presets and Z-level set pieces that feed the current OpenGL ES renderer.

Reference observations only:

- `01`: clean neon/isometric multi-level city block;
- `02`: vertical/cutaway district showing multiple Z-levels;
- `03`: biocorrupted/overgrown environmental variation;
- `04`: denser street/environment variation with heavy clutter and overgrowth.

The observed labels are visual descriptions, not gameplay semantics. Gameplay state remains owned by the existing simulation.
