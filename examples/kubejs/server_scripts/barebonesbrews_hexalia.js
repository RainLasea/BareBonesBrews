ServerEvents.recipes(event => {
  if (!Platform.isLoaded('hexalia')) return

  event.remove({ id: 'barebonesbrews:hexalia_rough/potion/minecraft/strength' })
  event.custom({
    type: 'hexalia:small_cauldron',
    ingredients: [{ item: 'minecraft:amethyst_shard' }],
    result: {
      id: 'barebonesbrews:rough_potion',
      count: 1,
      components: {
        'barebonesbrews:source_potion': 'minecraft:strength',
        'minecraft:potion_contents': {
          custom_effects: [{ id: 'minecraft:strength', amplifier: 0, duration: 1200 }]
        }
      }
    },
    duration: 200
  }).id('my_pack:hexalia_strength')
})
