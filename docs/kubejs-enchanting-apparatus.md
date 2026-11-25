# Biome-aware Enchanting Apparatus recipes

Register recipes in `kubejs/server_scripts/` through `ServerEvents.recipes`. Use `event.custom({...}).id('pack:recipe_id')` with `arsjsbiomescripting:biome_enchanting_apparatus` for a biome-restricted recipe. Use `ars_nouveau:enchanting_apparatus` for the unrestricted variant.

| Field | Type | Required | Default | Meaning |
| --- | --- | --- | --- | --- |
| `reagent` | Ars ingredient object | Yes | None | Ingredient offered to the Apparatus. |
| `result` | Item stack object | Yes | None | Item produced by the craft. |
| `pedestalItems` | Array of Ars ingredient objects | Yes | None | Ingredients on nearby pedestals. The order of the entries does not matter. |
| `sourceCost` | Integer | Yes | None | Source drawn when the craft starts. |
| `keepNbtOfReagent` | Boolean | Yes | None | Whether Ars copies the reagent's item components onto the result. |
| `biomes` | Array of biome ID strings | For the restricted serializer | None | Non-empty list of allowed biomes. Use an array even for one biome. |

The Apparatus checks the biome at its own position before charging Source. A matching biome recipe takes priority over an unrestricted recipe. If the biome-specific recipe can no longer match while crafting, the craft stops and preserves the reagent and pedestal items. Source already drawn at the start stays spent.

For identical reagent and pedestal ingredients, two restricted recipes can use disjoint biome lists. If their lists overlap, KubeJS keeps the first recipe and reports the conflicting IDs and biomes in `logs/kubejs/server.log`. Imbuement and Apparatus recipes are checked independently.

## Try the example

```js
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
```

Copy the [full example](../examples/kubejs/server_scripts/biome_enchanting_apparatus.js) into `kubejs/server_scripts/`. Place a snowball and an amethyst shard on two nearby pedestals, then offer an ice block to the Apparatus. In Snowy Plains it should make blue ice for 400 Source. Elsewhere the same ingredients should make packed ice for 200 Source.

JEI shows both recipes in Ars's Enchanting Apparatus category. The blue ice recipe shows its biome beneath the Source cost. Hover over the biome row for the full list on recipes that allow multiple biomes.
