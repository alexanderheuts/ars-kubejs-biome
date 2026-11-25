# Ars Nouveau: KubeJS Biome Scripting

Ars Nouveau: KubeJS Biome Scripting is a NeoForge addon for modpack makers. It lets you use KubeJS to define Ars Nouveau rituals that change biomes or shape terrain, and crafting recipes whose results depend on where players build their machines.

**Minecraft 1.21.1 · NeoForge**

**Authors** Holysweet and AquariusSidhe

**License** [MIT](https://github.com/alexanderheuts/ars-kubejs-biome/blob/main/LICENSE)

## Custom rituals

Players use the Ars Nouveau Ritual Brazier with the item you choose. A ritual can work in any biome or require specific starting biomes. The same item can trigger different rituals in different biomes, so you can chain biome changes as part of your pack's progression.

| Mode | What it does |
| --- | --- |
| `BiomeOnly` | Changes the biome and leaves the terrain intact. |
| `Islands` | Fills air with the configured blocks. This is the default mode. |
| `Replace` | Replaces occupied positions and leaves air gaps open. |
| `ReplaceAndFill` | Replaces occupied positions and fills air gaps. |

- **Shape the terrain.** Set the radius and up to four depth bands in a footprint up to seven layers deep. Omitted bands inherit the block from the band above. Layers can use solid blocks or fluid sources. Placement rituals can also change the biome, or preserve it.
- **Set the Source cost.** Choose a cost per eligible position. The brazier pays from nearby Source Jars as it works and pauses until it can afford the next batch.
- **Add augments.** Source Gems increase radius by default. Define your own augment items to change the radius, target biome, placement mode, block layers, or Source cost.

## Biome-aware crafting

- Add biome requirements to **Enchanting Apparatus** and **Imbuement Chamber** recipes. The machine checks the biome at its own position before spending Source.
- Give the same ingredients different outputs in different biomes, with an unrestricted recipe as a fallback. KubeJS reports conflicting biome assignments.
- With **JEI**, biome requirements appear in Ars Nouveau's existing recipe categories.

## Requirements

Install [Ars Nouveau](https://www.curseforge.com/minecraft/mc-mods/ars-nouveau) and [KubeJS](https://www.curseforge.com/minecraft/mc-mods/kubejs) alongside the addon. [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) is optional. The development baseline is NeoForge 21.1.267, Ars Nouveau 5.13.5, and KubeJS 2101.7.2-build.381.

## Documentation and examples

- Configure inputs, modes, biome checks, layers, and Source costs. [Ritual API](https://github.com/alexanderheuts/ars-kubejs-biome/blob/main/docs/kubejs.md)
- Set up built-in defaults, shared settings, and per-item overrides. [Ritual augments](https://github.com/alexanderheuts/ars-kubejs-biome/blob/main/docs/kubejs-augments.md)
- Define a Desert recipe and its unrestricted fallback. [Biome-aware imbuement](https://github.com/alexanderheuts/ars-kubejs-biome/blob/main/docs/kubejs-imbuement.md)
- Define a Snowy Plains recipe and its unrestricted fallback. [Biome-aware enchanting](https://github.com/alexanderheuts/ars-kubejs-biome/blob/main/docs/kubejs-enchanting-apparatus.md)
- Try a layered Badlands island, an Archwood augment, or water and lava generators. [Ready-to-copy KubeJS scripts](https://github.com/alexanderheuts/ars-kubejs-biome/tree/main/examples/kubejs/server_scripts)

To try an example, copy its script into your instance's `kubejs/server_scripts/` folder, then open a world or run `/reload`.
