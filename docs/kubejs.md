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
| `targetBiome` | Biome ID string | Yes | None | Registered biome applied in the ritual footprint. |
| `defaultMode` | String | No | `'Islands'` | `'Islands'` fills air with the configured blocks. `'BiomeOnly'` changes the biome without placing blocks. |
| `blocks` | Object | For `Islands` only | None | Block IDs for the depth bands listed below. |
| `radius` | Integer from 1 to 64 | No | `7` | Radius in blocks used to select positions around the brazier. The biome change uses Ars Nouveau's biome placement helper. |
| `sourceCostPerBlock` | Integer from 0 to 429496729 | No | `10` | Source charged per processed position. `0` makes the ritual free. |

Use specific item and biome IDs. For a minimal runnable script with unrestricted starting biomes, see the [sand-to-desert example](../examples/kubejs/server_scripts/sand_to_desert.js).

## Define island layers

For `Islands`, set `blocks.layer1` to a registered, non-air block ID. Use the optional keys to change the material farther below the brazier.

| Key | Depth below the brazier | Required | Default |
| --- | --- | --- | --- |
| `layer1` | 1 | Yes | None |
| `layer2` | 2 | No | `layer1` |
| `layer3To4` | 3–4 | No | Resolved `layer2` |
| `layer5Plus` | 5–7 | No | Resolved `layer3To4` |

The [terracotta island example](../examples/kubejs/server_scripts/terracotta_island.js) uses four colors and targets Badlands. It omits `startingBiomes` so an ordinary terracotta block can load it in any biome.

## Use the brazier

1. With the input in your main hand, right-click an empty Ars Nouveau Ritual Brazier to load the ritual. In survival this uses one item. Breaking the brazier before starting returns that item.
2. Right-click the brazier with an empty main hand to start. If the input has no matching ritual for the biome, the brazier stays idle and the item stays in your hand when you try to load it.
3. The ritual preserves existing terrain and fluids. `Islands` fills air and applies the target biome at positions where it places blocks. `BiomeOnly` applies the target biome directly. The brazier pays up front in batches of up to five positions, skipping occupied positions for `Islands`. At the default cost, a full batch costs 50 Source. If nearby jars cannot cover a batch, the brazier pauses and retries automatically. A partial final batch costs only as much as the positions it processes.

Definitions are looked up again when a loaded ritual starts. A running ritual saves its resolved settings and progress, so scripts can change without changing that run.

## Registration errors

Definitions may share an input item if their starting-biome lists are disjoint. Omitting `startingBiomes` covers all biomes for this check. If an ID is already registered or two definitions for the same item cover the same starting biome, the first stays and KubeJS rejects the later definition. Invalid option values are also reported in `logs/kubejs/server.log`. Other valid definitions can still register.
