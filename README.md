# BareBonesBrews / 粗简制药

Minecraft 1.21.1 · NeoForge 21.1 · Java 21

BareBonesBrews lets players brew weaker versions of installed potions in a vanilla water cauldron.
It discovers the live potion and brewing registries, so ordinary modded potions need no compatibility
list.

## Gameplay

1. Fill a vanilla cauldron to level 3.
2. Add one item from `#barebonesbrews:rough_base` (brown/red mushrooms by default).
3. Add the potion's normal brewing ingredient.
4. Put a block from `#barebonesbrews:cauldron_heat_sources` under the cauldron.
5. When brewing finishes, use a glass bottle to take the result.

The cauldron keeps its ingredients and progress across saves. Removing the heat pauses progress.
Emptying or replacing the cauldron discards its brew.

Ingredients that lead to several possible potions are intentionally omitted. For example, redstone,
glowstone and fermented spider eye cannot identify one output without an input potion, so generating
those recipes would make the result depend on recipe ordering.

## Architecture

- One `barebonesbrews:rough_potion` item represents every variant. The stack stores its source potion
  in `barebonesbrews:source_potion` and its diluted effects in `minecraft:potion_contents`.
- No potion or item registry is modified dynamically. This makes mod loading deterministic and allows
  common-config values to affect generated stacks.
- A narrow Mixin gives vanilla cauldrons a block entity. That block entity is the sole persisted and
  synchronized brew state; there is no duplicate `SavedData` mirror.
- The vanilla cauldron model renders the liquid throughout. On completion its existing animated water
  face is tinted to the potion color, while the spent ingredients spiral below the surface and fade
  from view naturally.
- Audio is tied to lifecycle transitions: quiet splashes while adding ingredients, a subdued start
  cue, occasional simmering pops and the normal bottle-fill sound. Completion itself is silent.
- A built-in in-memory data pack provides one component-aware cauldron recipe per unambiguous potion.
- When Hexalia is installed, those recipes are emitted as native `hexalia:small_cauldron` recipes and
  the vanilla cauldron mixin is disabled. Hexalia owns the complete cooking, stirring and bottling flow.
- Two accessor Mixins read the finished vanilla brewing table. There is no reflection or private
  method-handle access.

The renderer's liquid and floating-item approach was informed by Hexalia's Small Cauldron. The
component-first potion representation and cauldron interaction flow were informed by Toil and
Trouble. Neither mod is a dependency and no source is copied into the output.

## Configuration

`config/barebonesbrews-common.toml`:

```toml
enable_rough_potions = true
duration_multiplier = 0.5
round_duration_up = true
min_duration_ticks = 200
amplifier_reduction = 1
min_amplifier = 0
excluded_namespaces = []
enable_cauldron_recipes = true
show_summary_message = true
```

Runtime logging is limited to startup summaries and failures. Development runs enable verbose
NeoForge logging through Gradle, but no debug dumper or per-potion generated asset ships in the jar.

## Build

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

The release jar is written to `build/libs/`.
