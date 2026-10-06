# NeonMarshal enemy sprite variants

## Purpose

Old sprite sheets are treated as visual variants, not as new tactical entity types. The tactical archetype remains owned by NanoMarshal AI; the sprite variant is presentation metadata that can be swapped without changing behavior.

## Source sheets

| Variant ID | Visual role | Useful animation states | Likely tactical archetypes |
| --- | --- | --- | --- |
| `heavy_elite` | Armored elite/operator | idle, walk east/west, arm weapon attack, blade attack, special/action, hurt, death, fall | `SHIELD_ENFORCER`, `BOUNTY_BOSS` |
| `sewer_mutant` | Heavy sewer/industrial mutant | idle, walk east/west, ranged attack, claw attack, lurk, special, hurt, death, fall | `GRUNT` variant, future mutant archetype |
| `stalker_beast` | Agile creature-like stalker | idle, walk/stalk east/west, leap, claw attack, gas throw, hurt, death, fall | `FLANKER`, future stalker archetype |
| `hooded_operator` | Agile masked operative | idle, walk east/west, melee, ranged attack, climb, jump, block, hurt, death, fall | `FLANKER`, `SNIPER_STALKER` |

## Integration rule

The renderer should resolve:

```
EnemyType / tactical state
        |
        v
visualVariantId
        |
        v
sprite animation profile
```

The variant must never determine AI ownership, semantic-world authority, or tactical rules.

## State mapping

The existing `AIState` remains the gameplay state machine. Sprite animation state is a presentation layer:

- `PATROL`, `SUSPICIOUS`, `INVESTIGATING` -> idle/walk depending velocity.
- `FLANKING`, `SEEKING_COVER`, `RETREAT` -> walk/run.
- `ENGAGED`, `SUPPRESSING` -> attack animation selected by the emitted tactical action.
- `STUNNED` -> hurt/stagger.
- `DEAD` -> death/fall.
- traversal/combat specials such as climb, jump, block, leap and secondary attacks remain explicit presentation actions rather than new AI states.

## Import status

The four supplied sheets are reference assets only at this stage. They contain presentation labels and a light background, so they are not yet production-ready transparent sprite atlases. Production import should crop individual frames, remove the sheet background/labels, preserve pixel edges, and place the resulting atlases in the Android resource layer.

## Design constraint

Do not create four hard-coded enemy classes around these sheets. The same visual variant may be reused by several tactical archetypes, and an archetype must remain functional when no sprite asset is available.
