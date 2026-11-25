# KubeJS biome ritual API

Put ritual definitions in your instance's `kubejs/server_scripts/` directory. The addon exposes `BiomeRitualEvents.register` in server scripts. Its registration callback runs when the server starts and after a successful datapack reload. Use `/reload` to pick up changes while a world is open.

## Register a biome-only ritual

```js
BiomeRitualEvents.register(event => {
  event.ritual('mypack:sand_to_desert', {
    name: 'Create Desert Biome',
    description: 'Converts the area to Desert without replacing terrain.',
    input: 'minecraft:sand',
    startingBiomes: ['minecraft:plains', 'minecraft:savanna'],
    targetBiome: 'minecraft:desert',
    defaultMode: 'BiomeOnly',
    radius: 7,
    sourceCostPerBlock: 10
  })
})
```

Call `event.ritual(id, options)` once for each definition. Use a unique, stable resource ID such as `mypack:sand_to_desert`. The ID identifies the definition in saved ritual state and in registration errors. The first argument is separate from the options object. Existing scripts can still call `event.biomeOnly(id, options)` to register a fixed `BiomeOnly` ritual.

| Option | Type | Required | Default | Meaning |
| --- | --- | --- | --- | --- |
| `name` | String | Yes | None | Non-empty name shown on the brazier. |
| `description` | String | Yes | None | Non-empty description shown in the brazier tooltip. |
| `input` | Item ID string | Yes | None | A registered, non-air item offered to an empty Ritual Brazier, such as `minecraft:sand`. |
| `startingBiomes` | Array of biome ID strings | No | Any biome | Non-empty list of biomes where the ritual can be loaded and started. The addon checks the biome at the brazier. Use an array even for one biome. |
| `targetBiome` | Biome ID string | For `BiomeOnly` | None | Registered biome applied in the ritual footprint. A placement ritual with this option omitted leaves the biome unchanged. |
| `defaultMode` | String | No | `'Islands'` | `'Islands'` fills air. `'Replace'` replaces non-air positions. `'ReplaceAndFill'` replaces non-air positions and fills air. `'BiomeOnly'` changes the biome without placing blocks. |
| `blocks` | Object | For `Islands`, `Replace`, and `ReplaceAndFill` | None | Block IDs for the depth bands listed below. |
| `radius` | Integer from 1 to 64 | No | `7` | Radius in blocks used to select positions around the brazier. The biome change uses Ars Nouveau's biome placement helper. |
| `sourceCostPerBlock` | Integer from 0 to 429496729 | No | `10` | Source charged per processed position. `0` makes the ritual free. |
| `radiusAugment` | Object or `false` | No | Source Gem, +1 per item, up to 64 items | Configures radius augments. Use `false` to disable them for a ritual. |
| `augments` | Array of objects | No | Empty array | Optional item-specific overrides applied when the items are offered to the staged brazier. |

Use specific item and biome IDs. For a minimal runnable script with unrestricted starting biomes, see the [sand-to-desert example](../examples/kubejs/server_scripts/sand_to_desert.js).

## Define placement layers

For placement modes, set `blocks.layer1` to a registered, non-air block ID. Use the optional keys to change the material farther below the brazier. All three placement modes use the same footprint.

| Key | Depth below the brazier | Required | Default |
| --- | --- | --- | --- |
| `layer1` | 1 | Yes | None |
| `layer2` | 2 | No | `layer1` |
| `layer3To4` | 3–4 | No | Resolved `layer2` |
| `layer5Plus` | 5–7 | No | Resolved `layer3To4` |

The [terracotta island example](../examples/kubejs/server_scripts/terracotta_island.js) uses four colors and targets Badlands. It omits `startingBiomes` so an ordinary terracotta block can load it in any biome.

The [Plains replacement example](../examples/kubejs/server_scripts/replace_with_plains.js) uses a grass block to replace occupied positions with grass and dirt. Try it on a small test structure with open gaps so you can check that those gaps stay air.

The [Swamp replacement and fill example](../examples/kubejs/server_scripts/replace_and_fill_swamp.js) uses a clay ball to replace occupied positions and fill air with grass and mud. In a test structure, both the original blocks and the gaps should change within the footprint.

### Place fluids without changing the biome

Use a registered liquid block ID such as `minecraft:water` or `minecraft:lava` for a layer and omit `targetBiome`. The ritual places source blocks at eligible positions. Normal fluid flow can carry liquid beyond the placement footprint.

The [fluid generator examples](../examples/kubejs/server_scripts/fluid_generators.js) define two standalone rituals:

| Ritual | Input | Mode | Radius | Source per eligible position |
| --- | --- | --- | --- | --- |
| Water source | Water bucket | `Islands` | 3 | 10 |
| Lava pit | Magma cream | `Replace` | 5 | 100 |

The water ritual consumes the loaded water bucket in survival, like any other ritual input. The lava pit only replaces occupied positions. Both leave the biome unchanged.

## Configure radius augments

Load a ritual into the brazier, then right-click with the augment item in your main hand before starting. The brazier accepts one item per offer. It can also take augments dropped nearby, following normal Ars behavior. Increases add to the base radius, and the number of accepted items is capped per ritual. The base layer depth stays fixed.

```js
radiusAugment: {
  item: 'minecraft:amethyst_shard',
  increase: 2,
  maxCount: 8
}
```

| Option | Type | Required | Default |
| --- | --- | --- | --- |
| `item` | Registered, non-air item ID string | No | `'ars_nouveau:source_gem'` |
| `increase` | Positive integer | No | `1` |
| `maxCount` | Positive integer | No | `64` |

Omitting the entire object uses all three defaults. Breaking a staged brazier returns the accepted augment items along with the consumed ritual input. Once the ritual starts, its resolved radius is saved with the run.

## Override settings by augment item

Each entry in `augments` has an `item` ID and an `overrides` object. The optional `maxCount` is a positive integer and defaults to `1`. A ritual can use each custom augment item once in its definition. If a custom augment uses the same item as the radius augment, the custom rule takes precedence for that item.

```js
augments: [
  {
    item: 'minecraft:amethyst_shard',
    overrides: {
      targetBiome: 'ars_nouveau:archwood_forest',
      mode: 'Islands',
      blocks: {
        layer1: 'ars_nouveau:purple_archwood_log',
        layer2: 'ars_nouveau:stripped_purple_archwood_log'
      }
    }
  },
  {
    item: 'minecraft:copper_ingot',
    overrides: { sourceCostPerBlock: 0 }
  }
]
```

Supported override keys are `targetBiome`, `radius`, `sourceCostPerBlock`, `mode`, and `blocks`. Every key is optional, but an entry must supply an override. `blocks` can name any of the four layer keys. An augment switching to a placement mode needs a top-layer block from the base ritual or that augment. An augment switching to `BiomeOnly` likewise needs a target biome from the base ritual or that augment.

Offers affecting different settings combine, and the last offer to set a value wins for that setting or individual block layer. Block inheritance runs after the overrides. A custom `radius` replaces the radius accumulated so far, and any radius augment offered afterward adds to it. The starting biome and ritual input still select the base ritual. See the [augment example](../examples/kubejs/server_scripts/ritual_augments.js) for a loadable script.

## Use the brazier

1. With the input in your main hand, right-click an empty Ars Nouveau Ritual Brazier to load the ritual. In survival this uses one item. Breaking the brazier before starting returns that item.
2. Right-click the brazier with an empty main hand to start. If the input has no matching ritual for the biome, the brazier stays idle and the item stays in your hand when you try to load it.
3. `Islands` fills air and preserves existing terrain and fluids. `Replace` changes occupied positions and preserves air. `ReplaceAndFill` changes both. Both replacement modes can overwrite fluids and player builds. Placement modes apply the target biome at eligible placement positions, while `BiomeOnly` applies it directly. The brazier pays up front in batches of up to five eligible positions. At the default cost, a full batch costs 50 Source. If nearby jars cannot cover a batch, the brazier pauses and retries automatically. A partial final batch costs only as much as the positions it processes.

Definitions are looked up again when a loaded ritual starts. A running ritual saves its resolved settings and progress, so scripts can change without changing that run.

## Registration errors

Definitions may share an input item if their starting-biome lists are disjoint. Omitting `startingBiomes` covers all biomes for this check. If an ID is already registered or two definitions for the same item cover the same starting biome, the first stays and KubeJS rejects the later definition. Invalid option values are also reported in `logs/kubejs/server.log`. Other valid definitions can still register.
