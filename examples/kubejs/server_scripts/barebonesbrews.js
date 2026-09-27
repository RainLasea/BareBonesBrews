// Install this OR the example datapack; both examples replace the strength recipe.
ServerEvents.recipes(event => {
  if (Platform.isLoaded('hexalia')) return

  const strength = Item.of({
    id: 'barebonesbrews:rough_potion',
    count: 1,
    components: {
      'barebonesbrews:source_potion': 'minecraft:strength',
      'minecraft:potion_contents': {
        custom_effects: [{ id: 'minecraft:strength', amplifier: 0, duration: 1200 }]
      }
    }
  })

  event.remove({ id: 'barebonesbrews:rough_brewing/potion/minecraft/strength' })
  event.recipes.barebonesbrews.rough_brewing(
    strength, ['#c:mushrooms', 'minecraft:amethyst_shard']
  ).duration(200).id('my_pack:custom_strength')

  event.replaceInput(
    { id: 'barebonesbrews:rough_brewing/potion/minecraft/swiftness' },
    'minecraft:sugar', 'minecraft:honey_bottle'
  )
  event.replaceOutput(
    { id: 'barebonesbrews:rough_brewing/potion/minecraft/leaping' },
    'barebonesbrews:rough_potion', strength
  )
})

ServerEvents.tags('block', event => {
  event.add('barebonesbrews:heat_sources', 'minecraft:diamond_block')
  event.remove('barebonesbrews:heat_sources', 'minecraft:magma_block')
})
