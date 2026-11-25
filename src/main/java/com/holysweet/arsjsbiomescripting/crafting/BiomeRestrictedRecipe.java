package com.holysweet.arsjsbiomescripting.crafting;

import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public interface BiomeRestrictedRecipe {
    BiomeRequirement biomeRequirement();

    Ingredient primaryIngredient();

    List<Ingredient> pedestalIngredients();
}
