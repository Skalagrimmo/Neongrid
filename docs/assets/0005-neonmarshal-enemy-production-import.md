# NeonMarshal enemy sprite production import

The four supplied labelled reference sheets are now converted into transparent, nearest-neighbor pixel-art atlases:

- heavy_elite
- sewer_mutant
- stalker_beast
- hooded_operator

The runtime checks asset availability through Android AssetManager. This means a build without the four binary files safely keeps the existing primitive enemy fallback; once the files are present, the OpenGL ES 3.0 sprite path activates automatically.

Required files under `app/src/main/assets/sprites/enemies/`:

```
heavy_elite_atlas.webp
sewer_mutant_atlas.webp
stalker_beast_atlas.webp
hooded_operator_atlas.webp
atlas.json
```

Atlas geometry is 6 x 6 cells, each 224 x 176 px. All transparent pixels were removed from the supplied sheets and the source pixel edges were kept at nearest-neighbor sampling.

The imported sheets are presentation assets only. NanoMarshal AI, EnemyAiStateMachine, combat rules, alert logic, and pathfinding remain independent.

The four binary files are supplied as a separate asset package because the current remote Git workflow cannot directly transfer conversation-uploaded binary files into the Windows working tree. The source-controlled code is already prepared to consume them without further architecture changes.
