
## Biome-only ritual test

See the [KubeJS API reference](docs/kubejs.md) for registration, supported parameters, and defaults.

The [sand-to-desert example](examples/kubejs/server_scripts/sand_to_desert.js) registers a biome-only ritual through `BiomeRitualEvents.register`. Copy the script into the instance's `kubejs/server_scripts` directory and open a world or use `/reload`.

1. Place an empty Ars Nouveau Ritual Brazier in a biome other than Desert so the change is visible.
2. Right-click it with vanilla sand to load the ritual, then right-click with an empty main hand to start it.
3. For the Source check, start with no charged Source Jar nearby. The ritual should wait. Supply Source within six blocks of the brazier and check that it resumes. The example charges 10 Source per processed position, up to 50 per batch.
4. Check the biome with F3 near the brazier and confirm the terrain stayed intact.

The example works from any starting biome. To test biome filtering, add `startingBiomes: ['minecraft:plains']` to the definition, reload, and try a fresh brazier outside Plains. It should leave the sand in your hand.

Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
