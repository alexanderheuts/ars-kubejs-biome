BiomeRitualEvents.register(event => {
  event.ritual('arsjsbiomescripting:terracotta_island', {
    name: 'Create Terracotta Island',
    description: 'Conjures a layered terracotta island and changes the area to Badlands.',
    input: 'minecraft:terracotta',
    targetBiome: 'minecraft:badlands',
    blocks: {
      layer1: 'minecraft:yellow_terracotta',
      layer2: 'minecraft:orange_terracotta',
      layer3To4: 'minecraft:red_terracotta',
      layer5Plus: 'minecraft:brown_terracotta'
    }
  })
})
