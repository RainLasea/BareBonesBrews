# BareBonesBrews / 粗简制药

Brewing stands are precise. A water cauldron, a mushroom, and a little heat are not.

BareBonesBrews lets you make crude versions of potions in an ordinary cauldron. Crude potions are
shorter and weaker than their proper counterparts, but they are cheap to make and do not require
nether wart. The recipe list is built from the brewing registry at runtime, so potions added by other
mods usually work without a hand-written compatibility list.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.251 or newer
- Java 21

The mod is required on both the client and the server. It has no required content-mod dependency.

## Brewing in a cauldron

1. Fill a vanilla cauldron completely with water.
2. Add a mushroom from the shared `#c:mushrooms` item tag, including brown and red mushrooms.
3. Add the ingredients for the potion you want.
4. Place a block from `#farmersdelight:heat_sources` below the cauldron.
5. Wait for the brew to finish, then take it out with a glass bottle.

A full cauldron yields three bottles. By default, heat can come from a lit campfire, a lit soul
campfire, magma, lava, a lava cauldron, fire, or soul fire. An extinguished campfire does not heat
the brew. Farmer's Delight is optional: vanilla heat sources are supplied even without it, and
other mods can add compatible heaters through the same tag.

You can add the whole recipe at once or brew it one step at a time. A cauldron holds up to five
ingredients, counting the mushroom, and ingredient order does not matter. If the contents do not
match a recipe, the cauldron simply waits. Shift-right-click it before completion to take back the
oldest ingredient.

Once a brew is ready, you can add another brewing ingredient to process the remaining potion in
the cauldron. Shift-right-click during this step to recover that ingredient and restore the previous
brew, including after saving and reloading the world.

Removing the heat pauses brewing. Breaking, emptying, or replacing the cauldron discards its current
brew. Ingredients and progress are saved with the world.
Other changes to the water level, such as washing items or refilling with water, also discard the brew.

Drinkable crude potions can still be processed in a brewing stand. Creating and upgrading crude
splash and lingering potions requires the cauldron: add gunpowder for splash form, then dragon's
breath for lingering form. Whole-batch recipes can include both reagents within the five-ingredient
limit, or you can add them to a finished brew in stages.

## Compatibility

- **Modded potions:** brewing paths are discovered from the loaded potion recipes. Mods with unusual
  brewing systems may still need explicit support.
- **Jade:** shows the cauldron's ingredients in its tooltip.
- **JEI, REI and EMI:** add a Rough Brewing recipe category with five evenly spaced ingredient slots.
- **Hexalia:** uses Hexalia's small cauldron when it is installed instead of changing the vanilla
  cauldron. Recipes appear only in Hexalia's own category, omit the mushroom base, and use at most
  four ingredients. Recipe outputs preserve potion effects, including splash and lingering forms.

Jade, JEI, REI, EMI, and Hexalia are optional.

## Configuration

The common config is created at `config/barebonesbrews-common.toml`.

| Option | Default | Purpose |
| --- | ---: | --- |
| `enable_rough_potions` | `true` | Enables crude potion items and brewing-stand processing. |
| `duration_multiplier` | `0.5` | Multiplies the original effect duration. |
| `round_duration_up` | `true` | Rounds scaled durations up instead of down. |
| `min_duration_ticks` | `200` | Sets the minimum duration; 20 ticks are one second. |
| `amplifier_reduction` | `1` | Removes this many effect levels. |
| `min_amplifier` | `0` | Sets the lowest allowed amplifier; 0 means level I. |
| `excluded_namespaces` | `[]` | Skips potions from the listed namespaces. |
| `enable_cauldron_recipes` | `true` | Enables generated cauldron recipes. |

Duration and amplifier minimums never raise an effect above its original value. Instant effects keep
their original duration, and infinite effects remain infinite. Restart after changing configuration
to refresh all generated recipes and recipe-viewer entries; existing bottles retain their saved effects.

Pack authors can customize the two shared tags mentioned above to change the brewing base or valid
heat sources. Changes to these shared tags also affect other mods that use them. The former
`barebonesbrews:rough_base` and `barebonesbrews:cauldron_heat_sources` tags are no longer used.
The mod does not collect telemetry or contact an external service.

## Notes for developers

Crude potions store the source potion and diluted effects as data components; the mod does not create
one registered item for every potion. Cauldron recipes are provided through an in-memory data pack,
and cauldron state is saved and synchronized by a block entity attached through a small Mixin.

Build with:

```bash
./gradlew build
```

The finished jar is written to `build/libs/`.

`./gradlew test` runs the NeoForge regression suite, which also runs as part of `build`. Tests load
the generated data pack and Mixins, and cover recipe matching, heat, saved brewing state, bottling,
configuration switches, and lingering potion clouds.

## Credits and license

The cauldron presentation was informed by Hexalia's Small Cauldron, while parts of the interaction
flow were informed by Toil and Trouble. BareBonesBrews does not bundle code or assets from either
project.

BareBonesBrews is distributed as **All Rights Reserved**. The NeoForge MDK template files retain
their own license; see [`TEMPLATE_LICENSE.txt`](TEMPLATE_LICENSE.txt).
