BiomeRitualEvents.register(event => {
  event.ritual('arsjsbiomescripting:replace_and_fill_swamp', {
    name: 'Create Swamp Terrain',
    description: 'Replaces terrain and fills air with grass and mud, changing the area to Swamp.',
    input: 'minecraft:mud',
    targetBiome: 'minecraft:swamp',
    defaultMode: 'ReplaceAndFill',
    blocks: {
      layer1: 'minecraft:mud',
      layer2: 'minecraft:dirt'
    }
  })
})
