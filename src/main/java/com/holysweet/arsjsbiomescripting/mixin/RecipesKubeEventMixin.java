package com.holysweet.arsjsbiomescripting.mixin;

import com.google.gson.JsonElement;
import com.holysweet.arsjsbiomescripting.kubejs.BiomeRecipeValidation;
import dev.latvian.mods.kubejs.recipe.RecipesKubeEvent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = RecipesKubeEvent.class, remap = false)
@SuppressWarnings("unused") // Loaded from arsjsbiomescripting.mixins.json.
abstract class RecipesKubeEventMixin {
    @Inject(method = "applyChanges", at = @At("HEAD"))
    private void arsjsbiomescripting$validateBiomeRecipes(Map<ResourceLocation, JsonElement> recipes, CallbackInfo callback) {
        BiomeRecipeValidation.validate((RecipesKubeEvent) (Object) this);
    }
}
