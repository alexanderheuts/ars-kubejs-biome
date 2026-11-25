BiomeRitualEvents.register(event => {
  event.ritual('arsjsbiomescripting:sand_to_desert', {
    name: 'Create Desert Biome',
    description: 'Converts the area to Desert without replacing terrain.',
    input: 'minecraft:sand',
    targetBiome: 'minecraft:desert',
    defaultMode: 'BiomeOnly',
    radius: 7,
    sourceCostPerBlock: 10
  })
})
