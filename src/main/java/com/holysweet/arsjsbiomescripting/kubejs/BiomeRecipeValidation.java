package com.holysweet.arsjsbiomescripting.kubejs;

import com.google.common.collect.ImmutableMultiset;
import com.holysweet.arsjsbiomescripting.crafting.BiomeRecipeSerializers;
import com.holysweet.arsjsbiomescripting.crafting.BiomeRestrictedRecipe;
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

public final class BiomeRecipeValidation {
    private BiomeRecipeValidation() {}

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
        if (recipe.removed || recipe.creationError) {
            return;
        }
        var schema = recipe.getSerializationTypeFunction().schemaType;
        String category;
        if (BiomeRecipeSerializers.IMBUEMENT_ID.equals(schema.id)) {
            category = "imbuement";
        } else if (BiomeRecipeSerializers.APPARATUS_ID.equals(schema.id)) {
            category = "enchanting apparatus";
        } else {
            return;
        }

        recipe.serializeChanges();
        var id = recipe.getOrCreateId();
        var decoded = schema.getSerializer().codec().codec().parse(event.ops.json(), recipe.json);
        if (decoded.error().isPresent()) {
            reject(recipe, "Biome " + category + " recipe '" + id + "': " + decoded.error().orElseThrow().message()
                    + " in " + recipe.json);
            return;
        }

        if (!(decoded.getOrThrow() instanceof BiomeRestrictedRecipe restricted)) {
            return;
        }
        var inputs = new Inputs(schema.id, restricted.primaryIngredient(),
                ImmutableMultiset.copyOf(restricted.pedestalIngredients()));
        var biomes = restricted.biomeRequirement().keys();
        var byBiome = claims.computeIfAbsent(inputs, key -> new HashMap<>());
        var overlaps = biomes.stream().filter(byBiome::containsKey)
                .sorted(Comparator.comparing(ResourceKey::location))
                .map(biome -> biome.location() + " (recipe '" + byBiome.get(biome) + "')")
                .toList();
        if (!overlaps.isEmpty()) {
            reject(recipe, "Duplicate biome " + category + " recipe '" + id
                    + "': the same primary and pedestal inputs are already registered in " + String.join(", ", overlaps));
            return;
        }

        biomes.forEach(biome -> byBiome.put(biome, id));
    }

    private static void reject(KubeRecipe recipe, String message) {
        recipe.removed = true;
        ConsoleJS.SERVER.error(message, recipe.sourceLine, null, null);
    }

    private record Inputs(ResourceLocation serializer, Ingredient primary, ImmutableMultiset<Ingredient> pedestals) {}
}
