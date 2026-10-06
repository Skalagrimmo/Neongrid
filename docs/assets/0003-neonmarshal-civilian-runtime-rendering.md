# NeonMarshal civilian runtime and sprite pipeline

## Runtime flow

GameViewModel -> CivilianNpcSystem -> CivilianNpc / CivilianActivity -> GameCanvas -> GlIsoRenderer -> GlSpriteBatchRenderer -> pedestrians_atlas.webp

## Why this fits the current architecture

The project already uses Kotlin state models, LevelManager for grid truth, a main GameViewModel loop, Compose for screen composition, and OpenGL ES 3.0 for the isometric viewport. Civilians therefore do not introduce a second game engine or a separate rendering framework.

 CivilianNpcSystem is a lightweight ambient simulation layer. It uses LevelManager walkability for movement but does not enter EnemyAIManager or the combat state machine.

GlSpriteBatchRenderer is separate from the existing primitive GlBatchRenderer because textured sprites need UVs and a texture sampler, while the existing renderer is deliberately optimized around packed vertex colors. Both are OpenGL ES 3.0 and share the same viewport/projection coordinate space.

## Depth integration

The main isometric renderer already processes world geometry by increasing x + y depth. Civilian sprites are inserted at the same depth boundary: draw normal primitive geometry for the depth, flush it, draw that depth's civilian sprites in one textured batch, then continue with the next depth.

This preserves the renderer's existing ordering without introducing a full scene graph.

## Animation integration

PedestrianSpriteAtlas converts CivilianActivity and the NPC animation clock into a frame index. Horizontal direction is represented by the sprite quad flipX path, so duplicate east/west bitmap frames are not required.

The source metadata remains in app/src/main/assets/sprites/pedestrians/atlas.json for traceability, while Kotlin owns the hot-path lookup to avoid per-frame JSON parsing.

## Performance constraints

- one shared texture;
- one small dynamic sprite batch;
- nearest-neighbor texture sampling;
- no per-NPC bitmap allocation;
- animation state is scalar data on the existing NPC model;
- civilian simulation runs inside the existing game tick;
- the existing low-spec mode remains unchanged.

## Future extension

More civilian sheets can register new visualVariantId values without changing CivilianNpcSystem. Specialist profiles such as vendors can gain interaction points and dialogue/economy behavior later while continuing to reuse the same base NPC and rendering contracts.