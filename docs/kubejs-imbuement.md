# Biome-aware imbuement recipes

Register Imbuement Chamber recipes in `kubejs/server_scripts/` through `ServerEvents.recipes`. Use `event.custom({...}).id('pack:recipe_id')` with the serializer `arsjsbiomescripting:biome_imbuement` for a restricted recipe. Use `ars_nouveau:imbuement` for an unrestricted recipe.

The restricted serializer accepts the usual Ars imbuement fields and one extra field.

| Field | Type | Required | Default | Meaning |
| --- | --- | --- | --- | --- |
| `input` | Ars ingredient object | Yes | None | Ingredient placed in the chamber. |
| `output` | Item stack object | Yes | None | Item produced by imbuing. |
| `source` | Integer | Yes | None | Source cost passed to Ars. |
| `pedestalItems` | Array of Ars ingredient objects | Yes | None | Ingredients on nearby pedestals. Use `[]` when the recipe has no pedestal ingredients. |
| `biomes` | Array of biome ID strings | Yes for the restricted serializer | None | Non-empty list of allowed biomes. Use an array even for one biome. |

The chamber checks the biome at its own block position. When both a restricted recipe and a normal recipe match, the restricted one takes priority in its listed biomes. The ordinary recipe can act as a fallback elsewhere. The chamber keeps Ars's existing pedestal search and matching, including unordered pedestal ingredients and exact occupied pedestal counts.

For identical chamber and pedestal ingredients, restricted recipes must use disjoint biome lists. If a recipe overlaps an earlier one, KubeJS reports the recipe IDs and overlapping biomes in `logs/kubejs/server.log` and drops the later recipe. Changing the order of the pedestal ingredients does not make a new set of inputs.

## Try the example

```js
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
```

Copy the [clay imbuement example](../examples/kubejs/server_scripts/biome_imbuement.js) into `kubejs/server_scripts/` and use `/reload` if a world is open. Put sand, gravel, and a brick on three nearby pedestals. Put a clay ball in the chamber and supply Source.

- In Desert, the chamber makes orange terracotta.
- In other biomes, the same setup makes ordinary terracotta.
- To test a restricted-only recipe, remove the ordinary recipe from the script and reload. In other biomes, the chamber leaves the clay ball in place and does not draw Source for that recipe.

Both recipes appear in Ars's Imbuement Chamber category in JEI. Restricted recipes show a biome row below the Source cost. Hover over the row to see the complete biome list.
