package com.holysweet.arsjsbiomescripting.mixin;

import com.hollingsworth.arsnouveau.common.block.tile.EnchantingApparatusTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ApparatusRecipeInput;
import com.hollingsworth.arsnouveau.common.crafting.recipes.IEnchantingRecipe;
import com.hollingsworth.arsnouveau.setup.registry.RecipeRegistry;
import com.holysweet.arsjsbiomescripting.crafting.BiomeApparatusRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EnchantingApparatusTile.class, remap = false)
@SuppressWarnings("unused") // Loaded from arsjsbiomescripting.mixins.json.
abstract class EnchantingApparatusTileMixin {
    @Unique
    private static final String ARSJSBIOMESCRIPTING_PAID_RECIPE = "arsjsbiomescripting:paid_apparatus_recipe";

    @Shadow
    private int counter;

    @Unique
    @Nullable
    private ResourceLocation arsjsbiomescripting$paidRecipeId;

    @Unique
    private int arsjsbiomescripting$paidSourceCost;

    @Inject(method = "getRecipe", at = @At("HEAD"), cancellable = true)
    private void arsjsbiomescripting$selectRecipe(ItemStack catalyst, @Nullable Player player,
                                                 CallbackInfoReturnable<IEnchantingRecipe> callback) {
        EnchantingApparatusTile apparatus = (EnchantingApparatusTile) (Object) this;
        var level = apparatus.getLevel();
        if (level == null) {
            callback.setReturnValue(null);
            return;
        }
        if (apparatus.isCrafting) {
            // Ordinary crafts keep Ars's existing lookup. A paid biome recipe cannot switch to a fallback.
            if (arsjsbiomescripting$paidRecipeId != null) {
                var holder = level.getRecipeManager().byKey(arsjsbiomescripting$paidRecipeId).orElse(null);
                var input = new ApparatusRecipeInput(catalyst, apparatus.getPedestalItems(), player);
                if (holder != null && holder.value() instanceof BiomeApparatusRecipe restricted
                        && restricted.sourceCost() == arsjsbiomescripting$paidSourceCost
                        && restricted.matchesAt(input, level, apparatus.getBlockPos())) {
                    callback.setReturnValue(restricted);
                } else {
                    callback.setReturnValue(null);
                }
            }
            return;
        }
        ApparatusRecipeInput input = null;
        for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeRegistry.APPARATUS_TYPE.get())) {
            if (holder.value() instanceof BiomeApparatusRecipe restricted) {
                if (input == null) {
                    input = new ApparatusRecipeInput(catalyst, apparatus.getPedestalItems(), player);
                }
                if (restricted.matchesAt(input, level, apparatus.getBlockPos())) {
                    callback.setReturnValue(restricted);
                    return;
                }
            }
        }
    }

    @Redirect(method = "attemptCraft", at = @At(value = "INVOKE", target =
            "Lcom/hollingsworth/arsnouveau/common/block/tile/EnchantingApparatusTile;getRecipe"
                    + "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)"
                    + "Lcom/hollingsworth/arsnouveau/common/crafting/recipes/IEnchantingRecipe;"))
    private IEnchantingRecipe arsjsbiomescripting$rememberPaidRecipe(
            EnchantingApparatusTile apparatus, ItemStack catalyst, @Nullable Player player
    ) {
        var recipe = apparatus.getRecipe(catalyst, player);
        arsjsbiomescripting$paidRecipeId = null;
        var level = apparatus.getLevel();
        if (recipe instanceof BiomeApparatusRecipe restricted && level != null) {
            for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeRegistry.APPARATUS_TYPE.get())) {
                if (holder.value() == restricted) {
                    arsjsbiomescripting$paidRecipeId = holder.id();
                    arsjsbiomescripting$paidSourceCost = restricted.sourceCost();
                    break;
                }
            }
        }
        return recipe;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void arsjsbiomescripting$clearPaidRecipe(CallbackInfo callback) {
        EnchantingApparatusTile apparatus = (EnchantingApparatusTile) (Object) this;
        if (!apparatus.isCrafting && arsjsbiomescripting$paidRecipeId != null) {
            arsjsbiomescripting$paidRecipeId = null;
            counter = 0;
            apparatus.updateBlock();
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void arsjsbiomescripting$savePaidRecipe(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        EnchantingApparatusTile apparatus = (EnchantingApparatusTile) (Object) this;
        if (apparatus.isCrafting && arsjsbiomescripting$paidRecipeId != null) {
            var paidRecipe = new CompoundTag();
            paidRecipe.putString("id", arsjsbiomescripting$paidRecipeId.toString());
            paidRecipe.putInt("sourceCost", arsjsbiomescripting$paidSourceCost);
            tag.put(ARSJSBIOMESCRIPTING_PAID_RECIPE, paidRecipe);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void arsjsbiomescripting$loadPaidRecipe(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        var paidRecipe = tag.getCompound(ARSJSBIOMESCRIPTING_PAID_RECIPE);
        arsjsbiomescripting$paidRecipeId = paidRecipe.contains("id", Tag.TAG_STRING)
                ? ResourceLocation.tryParse(paidRecipe.getString("id")) : null;
        arsjsbiomescripting$paidSourceCost = paidRecipe.getInt("sourceCost");
    }
}
