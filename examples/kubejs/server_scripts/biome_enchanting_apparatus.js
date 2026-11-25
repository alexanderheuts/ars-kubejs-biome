ServerEvents.recipes(event => {
  event.custom({
    type: 'ars_nouveau:enchanting_apparatus',
    reagent: { item: 'minecraft:ice' },
    result: { id: 'minecraft:packed_ice', count: 1 },
    pedestalItems: [
      { item: 'minecraft:snowball' },
      { item: 'minecraft:amethyst_shard' }
    ],
    sourceCost: 200,
    keepNbtOfReagent: false
  }).id('arsjsbiomescripting:ice_to_packed_ice')

  event.custom({
    type: 'arsjsbiomescripting:biome_enchanting_apparatus',
    reagent: { item: 'minecraft:ice' },
    result: { id: 'minecraft:blue_ice', count: 1 },
    pedestalItems: [
      { item: 'minecraft:snowball' },
      { item: 'minecraft:amethyst_shard' }
    ],
    sourceCost: 400,
    keepNbtOfReagent: false,
    biomes: ['minecraft:snowy_plains']
  }).id('arsjsbiomescripting:snowy_plains_ice_to_blue_ice')
})
