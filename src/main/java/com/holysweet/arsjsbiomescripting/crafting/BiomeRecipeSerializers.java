package com.holysweet.arsjsbiomescripting.crafting;

import com.holysweet.arsjsbiomescripting.ArsNouveauKubeJSBiomeScripting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BiomeRecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ArsNouveauKubeJSBiomeScripting.MODID);

    public static final ResourceLocation IMBUEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(ArsNouveauKubeJSBiomeScripting.MODID, "biome_imbuement");
    public static final DeferredHolder<RecipeSerializer<?>, BiomeImbuementRecipe.Serializer> IMBUEMENT =
            SERIALIZERS.register(IMBUEMENT_ID.getPath(), BiomeImbuementRecipe.Serializer::new);
    public static final ResourceLocation APPARATUS_ID =
            ResourceLocation.fromNamespaceAndPath(ArsNouveauKubeJSBiomeScripting.MODID, "biome_enchanting_apparatus");
    public static final DeferredHolder<RecipeSerializer<?>, BiomeApparatusRecipe.Serializer> APPARATUS =
            SERIALIZERS.register(APPARATUS_ID.getPath(), BiomeApparatusRecipe.Serializer::new);

    private BiomeRecipeSerializers() {}

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
