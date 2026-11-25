package com.holysweet.arsjsbiomescripting.mixin.jei;

import com.hollingsworth.arsnouveau.client.jei.EnchantingApparatusRecipeCategory;
import com.hollingsworth.arsnouveau.client.jei.ImbuementRecipeCategory;
import com.holysweet.arsjsbiomescripting.crafting.BiomeRestrictedRecipe;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;

@Mixin(value = {ImbuementRecipeCategory.class, EnchantingApparatusRecipeCategory.class}, remap = false)
@SuppressWarnings("unused") // Loaded from arsjsbiomescripting.mixins.json.
abstract class BiomeRecipeCategoryMixin implements IRecipeCategory<Object> {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target =
            "Lmezz/jei/api/helpers/IGuiHelper;createBlankDrawable(II)Lmezz/jei/api/gui/drawable/IDrawableStatic;"), index = 1)
    private int arsjsbiomescripting$reserveBiomeRow(int height) {
        return height + 14;
    }

    @Override
    public void createRecipeExtras(@NotNull IRecipeExtrasBuilder builder, @NotNull Object recipe,
                                   @NotNull IFocusGroup focuses) {
        if (!(recipe instanceof RecipeHolder<?> holder) || !(holder.value() instanceof BiomeRestrictedRecipe restricted)) {
            return;
        }

        var biomeNames = restricted.biomeRequirement().keys().stream()
                .map(ResourceKey::location)
                .sorted()
                .map(id -> Component.translatableWithFallback(Util.makeDescriptionId("biome", id), id.toString()))
                .toList();
        var names = ComponentUtils.formatList(biomeNames, Component.literal(", "));
        String labelKey = biomeNames.size() == 1 ? "jei.arsjsbiomescripting.biome" : "jei.arsjsbiomescripting.biomes";
        Component label = Component.translatable(labelKey, names);

        var tooltip = new ArrayList<Component>();
        tooltip.add(Component.translatable("jei.arsjsbiomescripting.required_biomes"));
        tooltip.addAll(biomeNames);
        builder.addText(label, getWidth(), 10)
                .setPosition(0, getHeight() - 11)
                .setColor(0x404040)
                .setTooltip(tooltip);
    }
}
