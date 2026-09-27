# BareBonesBrews

[![Available for NeoForge](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/neoforge_vector.svg)](https://github.com/RainLasea/BareBonesBrews)

BareBonesBrews lets you brew potions in an ordinary cauldron, using mushrooms in place of nether wart.

The result is a **crude potion**. By default, most effects last half as long and lose one level of strength, down to a minimum of level I.

## Get brewing

1. Fill a cauldron completely with water and place a lit campfire underneath.
2. Right-click to add a red or brown mushroom, then the brewing ingredients for your potion.
3. Let it brew, then collect it with glass bottles. **One full cauldron makes three bottles.**

You can add the whole recipe at once, in any order. A cauldron holds up to five ingredients, including the mushroom. Before the brew is finished, sneak-right-click to take back the oldest ingredient.

Add gunpowder to a finished brew to make splash potions, then dragon's breath to make lingering potions.

## Playing with other mods

- Crude potion recipes are generated from existing brewing recipes, so potions added by other mods through the standard brewing system can work too.
- **JEI, REI, and EMI** show the recipes. **Jade** shows what's in the cauldron.
- With **Hexalia** installed, brewing moves to its small cauldron. Recipes appear in Hexalia's category, and you can leave out the mushroom.

## Configuration and recipes

Settings are in `config/barebonesbrews-common.toml`. You can change effect strength and duration,
or set `generate_cauldron_recipes = false` to supply your own recipes.

`cauldron_heat_sources` accepts block IDs, `#block_tags`, and state conditions such as
`minecraft:furnace[lit=true]`. Its default is `["#barebonesbrews:heat_sources"]`.
Edit that block tag with a datapack or KubeJS to add or remove heat sources. This setting does not affect Hexalia.

Recipes use `data/<namespace>/recipe/<path>.json` and the type `barebonesbrews:rough_brewing`.
KubeJS also supports `event.recipes.barebonesbrews.rough_brewing(result, ingredients)`.
Use the same recipe ID to override a default recipe, then run `/reload`.

- [Example datapack](examples/datapack): copy this folder into your world's `datapacks/` directory. It replaces the strength recipe and heat-source list.
- [KubeJS examples](examples/kubejs/server_scripts): copy the scripts into `kubejs/server_scripts/`. Includes a Hexalia example.

## Installation

For **Minecraft 1.21.1 / NeoForge**. Install on both the client and the server. All integrations listed above are optional.
