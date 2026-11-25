ServerEvents.recipes(event => {
  event.custom({
    type: 'ars_nouveau:imbuement',
    input: { item: 'minecraft:clay_ball' },
    output: { id: 'minecraft:terracotta', count: 1 },
    source: 200,
    pedestalItems: [
      { item: 'minecraft:sand' },
      { item: 'minecraft:gravel' },
      { item: 'minecraft:brick' }
    ]
  }).id('arsjsbiomescripting:clay_to_terracotta')

  event.custom({
    type: 'arsjsbiomescripting:biome_imbuement',
    input: { item: 'minecraft:clay_ball' },
    output: { id: 'minecraft:orange_terracotta', count: 1 },
    source: 200,
    pedestalItems: [
      { item: 'minecraft:sand' },
      { item: 'minecraft:gravel' },
      { item: 'minecraft:brick' }
    ],
    biomes: ['minecraft:desert']
  }).id('arsjsbiomescripting:desert_clay_to_orange_terracotta')
})
