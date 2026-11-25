BiomeRitualEvents.register(event => {
  event.ritual('arsjsbiomescripting:water_source', {
    name: 'Conjure Water Source',
    description: 'Places water sources in open space beneath the brazier.',
    input: 'minecraft:water_bucket',
    defaultMode: 'Islands',
    radius: 3,
    blocks: { layer1: 'minecraft:water' }
  })

  event.ritual('arsjsbiomescripting:lava_pit', {
    name: 'Conjure Lava Pit',
    description: 'Turns occupied terrain beneath the brazier into lava.',
    input: 'minecraft:magma_cream',
    defaultMode: 'Replace',
    radius: 5,
    sourceCostPerBlock: 100,
    blocks: { layer1: 'minecraft:lava' }
  })
})
