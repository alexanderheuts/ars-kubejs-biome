package com.holysweet.arsjsbiomescripting.mixin;

import com.hollingsworth.arsnouveau.api.imbuement_chamber.IImbuementRecipe;
import com.hollingsworth.arsnouveau.api.registry.ImbuementRecipeRegistry;
import com.hollingsworth.arsnouveau.common.block.tile.ImbuementTile;
import com.holysweet.arsjsbiomescripting.crafting.BiomeImbuementRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ImbuementTile.class, remap = false)
@SuppressWarnings("unused") // Loaded from arsjsbiomescripting.mixins.json.
abstract class ImbuementTileMixin {
    @Inject(method = "getRecipeNow", at = @At("HEAD"), cancellable = true)
    private void arsjsbiomescripting$preferBiomeRecipe(
            CallbackInfoReturnable<RecipeHolder<? extends IImbuementRecipe>> callback
    ) {
        ImbuementTile chamber = (ImbuementTile) (Object) this;
        var level = chamber.getLevel();
        if (level == null) {
            return;
        }
        for (var holder : ImbuementRecipeRegistry.INSTANCE.getRecipes()) {
            if (holder.value() instanceof BiomeImbuementRecipe recipe && recipe.matches(chamber, level)) {
                callback.setReturnValue(holder);
                return;
            }
        }
    }
}
