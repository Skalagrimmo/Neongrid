# Enemy sprite import gate

**Superseded by:** `0005-neonmarshal-enemy-production-import.md`.

The original gate was created before the four enemy reference sheets were supplied. The source sheets are now processed into transparent WebP atlases; the remaining step is placing those binary atlases into `app/src/main/assets/sprites/enemies/`.

The runtime still probes AssetManager at surface creation, so the application remains safe when the binary files are absent and automatically enables the sprite path when all four are present.
