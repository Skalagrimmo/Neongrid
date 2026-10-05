# ADR 0002: NeonMarshal subsystem boundaries

## Status

Proposed

## Context

NeonGrid already contains the Android-facing RPG, mission, progression, and persistence runtime.
NanoMarshal contains the tactical runtime: voxel terrain, destructible cover, combat, tactical AI, behavior trees, hazards, pathfinding, and the 60 FPS game tick.

The stable external tools are deliberately implemented in separate runtimes:

- PixelGen 1.0 — objective semantic world generation and dynamic world state.
- EBE 1.0 — subjective observation, memory, belief, knowledge, communication, reaction, and semantic action requests.
- FPE 1.0 — deterministic read-only world projection/view state.

The integration must preserve the authority boundaries already frozen by those tools.

## Decision

NeonMarshal will use the following ownership model:

```text
                         ┌──────────────────────┐
                         │      PixelGen        │
                         │ objective world truth│
                         └──────────┬───────────┘
                                    │
                        events + local observations
                                    │
                                    v
                         ┌──────────────────────┐
                         │        EBE           │
                         │ subjective cognition│
                         └──────────┬───────────┘
                                    │
                         semantic action request
                                    │
                                    v
                         ┌──────────────────────┐
                         │    NanoMarshal       │
                         │ tactical simulation  │
                         └──────────┬───────────┘
                                    │
                         gameplay state / results
                                    │
                                    v
                         ┌──────────────────────┐
                         │      Neongrid        │
                         │ RPG/meta/persistence │
                         └──────────────────────┘

                         FPE
                          │
                          └── read-only deterministic projection
```

### Rules

1. PixelGen remains the only authority for objective generated-world semantics.
2. EBE never becomes a second source of world truth.
3. EBE observations are not automatically knowledge; locality and provenance are preserved.
4. A semantic action request is intent, not authority. World mutation happens through the owning runtime.
5. NanoMarshal owns moment-to-moment tactical simulation: movement, combat, cover, AI, hazards, and tactical state.
6. Neongrid owns player-facing campaign state, progression, persistence, missions, inventory, and Android UI orchestration.
7. FPE is tooling/projection only and never mutates the canonical world.
8. Android UI must not directly depend on PixelGen/EBE/FPE implementation internals.

## Stable dependency baseline

| Component | Baseline |
| --- | --- |
| NanoMarshal | GitHub `main`, latest inspected commit before integration |
| Neongrid | GitHub `main`, base runtime |
| PixelGen | `1.0.0 stable` |
| EBE | `1.0.0 stable` |
| FPE | `1.0.0 stable` |

The stable package contracts are frozen externally. Integration code should depend on explicit adapters/contracts rather than copying their internal implementation into Android classes.

## First vertical slice

The first NeonMarshal slice will prove this loop:

```text
load campaign
  -> obtain semantic sector from PixelGen
  -> expose local evidence to EBE
  -> enter NanoMarshal tactical session
  -> resolve tactical event
  -> emit semantic result/event
  -> update campaign/progression state
  -> persist
  -> restore consistently
```

Narrative decoration and broad content generation are explicitly deferred until this loop is stable.

## Consequences

- Existing Neongrid UI remains untouched during the foundation stage.
- NanoMarshal can be integrated incrementally instead of replacing the current GameViewModel in one step.
- PixelGen/EBE/FPE can remain outside the Android runtime process initially and communicate through versioned serialized contracts.
- Future in-process integration can replace adapters without changing ownership semantics.
