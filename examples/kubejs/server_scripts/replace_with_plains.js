BiomeRitualEvents.register(event => {
  event.ritual('arsjsbiomescripting:replace_with_plains', {
    name: 'Replace Terrain with Plains',
    description: 'Replaces occupied positions with grass and dirt while leaving air spaces open.',
    input: 'minecraft:grass_block',
    targetBiome: 'minecraft:plains',
    defaultMode: 'Replace',
    blocks: {
      layer1: 'minecraft:grass_block',
      layer2: 'minecraft:dirt'
    }
  })
})
