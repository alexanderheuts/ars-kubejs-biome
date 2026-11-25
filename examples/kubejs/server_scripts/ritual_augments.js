BiomeRitualEvents.register(event => {
  event.ritual('arsjsbiomescripting:ritual_augments', {
    name: 'Create Windswept Gravelly Hills',
    description: 'Changes the biome to Windswept Gravelly Hills. An Amethyst Shard conjures an Archwood Forest island instead.',
    input: 'minecraft:flint',
    targetBiome: 'minecraft:windswept_gravelly_hills',
    defaultMode: 'Replace',
    blocks: {
      layer1: 'minecraft:gravel'
    },
    augments: [
      {
        item: 'minecraft:amethyst_shard',
        overrides: {
          mode: 'Islands',
          targetBiome: 'ars_nouveau:archwood_forest',
          blocks: {
            layer1: 'ars_nouveau:purple_archwood_log',
            layer2: 'ars_nouveau:stripped_purple_archwood_log'
          }
        }
      }
    ]
  })
})
