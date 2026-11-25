# KubeJS ritual augments

Load a ritual into the brazier, then right-click with an augment item in your main hand before starting. The brazier accepts one item per offer and can also take augments dropped nearby. Breaking a loaded brazier before starting returns the consumed input and augments. See the [ritual API reference](kubejs.md) for the initial ritual settings.

## Built-in radius defaults

Leave `radiusAugment` out of a ritual definition to use the addon's defaults. They apply to every addon ritual, in every mode. Set `radiusAugment: false` to disable the radius augment on a particular ritual.

| Field | Type | Required | Default |
| --- | --- | --- | --- |
| `item` | Registered, non-air item ID string | No | `'ars_nouveau:source_gem'` |
| `increase` | Positive integer | No | `1` |
| `maxCount` | Positive integer | No | `64` |

Each accepted item adds `increase` blocks to the radius. `maxCount` is the total number accepted per run. In placement modes, increasing the radius widens the footprint and keeps its seven-layer depth.

## Share settings across a pack's rituals

The API reads `radiusAugment` and `augments` from each definition. To give several rituals the same settings, declare shared JavaScript values and pass them to each definition. For example, save this whole script in `kubejs/server_scripts/`:

```js
const sharedRadiusAugment = {
  item: 'ars_nouveau:source_gem',
  increase: 2,
  maxCount: 8
}

const sharedAugments = [
  {
    item: 'minecraft:copper_ingot',
    overrides: { sourceCostPerBlock: 5 }
  }
]

BiomeRitualEvents.register(event => {
  event.ritual('mypack:flint_plains_to_desert', {
    name: 'Dry the Plains',
    description: 'Changes Plains to Desert.',
    input: 'minecraft:flint',
    startingBiomes: ['minecraft:plains'],
    targetBiome: 'minecraft:desert',
    defaultMode: 'BiomeOnly',
    radiusAugment: sharedRadiusAugment,
    augments: sharedAugments
  })

  event.ritual('mypack:flint_desert_to_forest', {
    name: 'Restore the Forest',
    description: 'Changes Desert to Forest. Amethyst can change it to Archwood Forest.',
    input: 'minecraft:flint',
    startingBiomes: ['minecraft:desert'],
    targetBiome: 'minecraft:forest',
    defaultMode: 'BiomeOnly',
    radiusAugment: sharedRadiusAugment,
    augments: sharedAugments.concat([
      {
        item: 'minecraft:amethyst_shard',
        overrides: { targetBiome: 'ars_nouveau:archwood_forest' }
      }
    ])
  })
})
```

Both rituals accept up to eight Source Gems at +2 radius each, and both accept copper to set the cost to 5 Source per position. The second ritual adds an amethyst option of its own. Both use flint, so once the first ritual changes the biome at the brazier to Desert, flint can load the second one. Definitions that omit `radiusAugment` elsewhere still use the built-in defaults.

## Override settings by item

Every custom augment in `augments` has an `item` ID and an `overrides` object. Its optional `maxCount` is a positive integer and defaults to `1` per run. Define each item once in a ritual's custom array. A custom rule for the configured radius item takes precedence when that item is offered.

| Override | Effect |
| --- | --- |
| `targetBiome` | Sets the target biome to a registered biome ID. |
| `mode` | Sets the active mode to `BiomeOnly`, `Islands`, `Replace`, or `ReplaceAndFill`. |
| `radius` | Sets the radius to an integer from 1 to 64, replacing the radius accumulated so far. Later radius augments add to that value. |
| `sourceCostPerBlock` | Sets the Source cost per eligible position. Use the same range as the [ritual's base cost](kubejs.md). |
| `blocks` | Overrides specific [placement layers](kubejs.md#define-placement-layers). Omitted layers keep their configured values, and layer inheritance runs after all overrides are applied. |

Supply at least one override per custom augment. The last accepted offer to change a setting wins for that setting. Changes to different settings or different block layers combine. A switch to a placement mode requires a top-layer block on the base ritual or on the switching augment. A switch to `BiomeOnly` requires a target biome from the base ritual or the switching augment.

The [flint and amethyst example](../examples/kubejs/server_scripts/ritual_augments.js) shows a mode, biome, and layers changing together. Flint loads the Windswept Gravelly Hills replacement ritual. Adding amethyst switches it to an Archwood Forest island with logs on top and stripped logs below.
