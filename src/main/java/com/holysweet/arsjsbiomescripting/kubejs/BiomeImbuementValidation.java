package com.holysweet.arsjsbiomescripting.kubejs;

import com.google.common.collect.ImmutableMultiset;
import com.holysweet.arsjsbiomescripting.crafting.BiomeImbuementRecipe;
import com.holysweet.arsjsbiomescripting.crafting.BiomeRecipeSerializers;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipesKubeEvent;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.biome.Biome;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class BiomeImbuementValidation {
    private BiomeImbuementValidation() {}

    public static void validate(RecipesKubeEvent event) {
        Map<Inputs, Map<ResourceKey<Biome>, ResourceLocation>> claims = new HashMap<>();
        // Existing datapack recipes precede script additions. KubeJS keeps additions in script order.
        event.originalRecipes.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> validateRecipe(event, entry.getValue(), claims));
        for (var recipe : event.addedRecipes) {
            if (recipe.newRecipe) {
                validateRecipe(event, recipe, claims);
            }
        }
    }

    private static void validateRecipe(RecipesKubeEvent event, KubeRecipe recipe,
                                       Map<Inputs, Map<ResourceKey<Biome>, ResourceLocation>> claims) {
        if (recipe.removed || recipe.creationError
                || !BiomeRecipeSerializers.IMBUEMENT_ID.equals(recipe.getSerializationTypeFunction().schemaType.id)) {
            return;
        }

        recipe.serializeChanges();
        var id = recipe.getOrCreateId();
        var decoded = BiomeRecipeSerializers.IMBUEMENT.get().codec().codec().parse(event.ops.json(), recipe.json);
        if (decoded.error().isPresent()) {
            reject(recipe, "Biome imbuement recipe '" + id + "': " + decoded.error().orElseThrow().message()
                    + " in " + recipe.json);
            return;
        }

        BiomeImbuementRecipe restricted = decoded.getOrThrow();
        var inputs = new Inputs(restricted.getInput(), ImmutableMultiset.copyOf(restricted.getPedestalItems()));
        var byBiome = claims.computeIfAbsent(inputs, key -> new HashMap<>());
        var overlaps = restricted.biomeKeys().stream().filter(byBiome::containsKey)
                .sorted(Comparator.comparing(ResourceKey::location))
                .map(biome -> biome.location() + " (recipe '" + byBiome.get(biome) + "')")
                .toList();
        if (!overlaps.isEmpty()) {
            reject(recipe, "Duplicate biome imbuement recipe '" + id
                    + "': the same chamber and pedestal inputs are already registered in " + String.join(", ", overlaps));
            return;
        }

        restricted.biomeKeys().forEach(biome -> byBiome.put(biome, id));
    }

    private static void reject(KubeRecipe recipe, String message) {
        recipe.removed = true;
        ConsoleJS.SERVER.error(message, recipe.sourceLine, null, null);
    }

    private record Inputs(Ingredient chamber, ImmutableMultiset<Ingredient> pedestals) {}
}
