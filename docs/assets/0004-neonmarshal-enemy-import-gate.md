# Enemy sprite import gate

The enemy presentation contract is now wired, but the four original binary sheets are not present in the active working tree.

Required source sheets:

- heavy_elite
- sewer_mutant
- stalker_beast
- hooded_operator

Until those source images are available, NeonMarshal deliberately keeps the existing OpenGL primitive enemy fallback. This avoids fabricating art and keeps the NanoMarshal AI/gameplay layer independent from asset availability.

Once the sheets are supplied, the importer should crop transparent frames, preserve nearest-neighbor sampling, record frame indices in `app/src/main/assets/sprites/enemies/atlas.json`, and set `EnemySpriteAtlas.isProductionReady()` to true only after the atlas validation test passes.