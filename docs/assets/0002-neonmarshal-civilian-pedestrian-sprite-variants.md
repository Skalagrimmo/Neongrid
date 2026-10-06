# NeonMarshal civilian pedestrian sprite variants

## Purpose

The supplied pedestrian sheets are treated as reusable civilian presentation profiles, not as separate gameplay entity classes. Civilian behavior belongs to the NeonMarshal simulation/NPC layer; the sprite profile only supplies visual variants and animation capabilities.

## Source sheet

| Profile | Visual role | Available states / actions | Intended use |
| --- | --- | --- | --- |
| `pedestrian_civilian` | Everyday city resident / passer-by | idle, walk east/west, contextual action, gazing/chatting, gesture, hurt, death, fall/static-down | ambient street population and non-combat civilians |
| `pedestrian_vendor` | Street vendor | idle, vendor stall, interaction loop | specialist civilian with a persistent point of interest |

The sheet also contains several visually distinct civilians in the idle/walk groups. These should be represented as visual variants under the same civilian behavior model rather than as hard-coded NPC types.

## Suggested visual variant IDs

```
civilian_01
civilian_02
civilian_03
civilian_04
civilian_05
civilian_06
civilian_07
vendor_01
```

The exact frame-to-ID mapping can be finalized during atlas extraction. Do not make gameplay logic depend on the numeric suffix.

## Integration rule

The renderer should resolve:

```
Civilian NPC archetype / activity
          |
          v
    visualVariantId
          |
          v
 civilian animation profile
```

A visual variant must never determine faction, AI ownership, hostility, dialogue authority, economy rules, or tactical state.

## Activity mapping

Ambient civilian behavior should use a lightweight activity state machine rather than the combat AI state machine:

- `IDLE` -> idle animation.
- `TRAVEL` -> walk loop selected from the current facing.
- `SOCIALIZE` -> gazing/chatting or gesture.
- `CONTEXT_ACTION` -> contextual action such as interacting with a terminal/sign/prop.
- `VENDOR` -> vendor idle plus interaction loop.
- `HURT` -> hurt/stagger presentation when the gameplay layer reports a valid damage event.
- `DOWNED` -> fall/static-down presentation.
- `DEAD` -> death presentation where the scene rules permit it.

The presentation state should follow simulation output; the sprite animation must not become a second source of truth for NPC state.

## Civilian variation

The pedestrian set is useful for reducing repetition in dense scenes:

- choose a visual variant independently from the civilian behavior profile;
- randomize starting phase in the walk loop;
- vary idle duration and turn/facing timing inside bounded ranges;
- keep social pairs/groups as a behavior decision, not as a special sprite type.

This allows many civilians to share one behavior implementation while still looking non-identical.

## Vendor integration

`pedestrian_vendor` should be modeled as a specialist civilian profile layered over the same base NPC contract.

Expected separation:

```
Civilian NPC
  + activity = VENDOR
  + pointOfInterestId
  + interaction capability
  + visualVariantId = vendor_01
```

The stall/cart is part of scene presentation and/or a point-of-interest representation; the vendor's visual asset should remain replaceable.

## Import status

The supplied image is a reference sheet, not a production-ready transparent atlas. It contains labels and a light sheet background. Production import should:

1. crop individual frames;
2. remove labels and sheet background;
3. preserve hard pixel edges and nearest-neighbor sampling;
4. group frames into animation atlases;
5. register the resulting profile/variant metadata in the Android resource layer.

The original sheet should remain in the asset/source archive for traceability.

## Design constraints

Do not create one Kotlin/Java class per sprite variant. A single civilian NPC implementation should select:

```
behavior profile
    +
activity state
    +
visualVariantId
    +
animation direction
```

The civilian simulation must remain functional even when a visual profile is unavailable, allowing placeholder rendering during development and tests.
