package com.holysweet.arsjsbiomescripting.crafting;

import com.hollingsworth.arsnouveau.common.crafting.recipes.ApparatusRecipeInput;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BiomeApparatusRecipe extends EnchantingApparatusRecipe implements BiomeRestrictedRecipe {
    private final BiomeRequirement biomeRequirement;

    private BiomeApparatusRecipe(EnchantingApparatusRecipe recipe, BiomeRequirement biomeRequirement) {
        super(recipe.reagent(), recipe.result(), recipe.pedestalItems(), recipe.sourceCost(), recipe.keepNbtOfReagent());
        this.biomeRequirement = biomeRequirement;
    }

    @Override
    public BiomeRequirement biomeRequirement() {
        return biomeRequirement;
    }

    @Override
    public Ingredient primaryIngredient() {
        return reagent();
    }

    @Override
    public List<Ingredient> pedestalIngredients() {
        return pedestalItems();
    }

    @Override
    public boolean matches(ApparatusRecipeInput input, Level level, @Nullable Player player) {
        // Ars's generic recipe input has no position. The apparatus supplies it through matchesAt.
        return false;
    }

    public boolean matchesAt(ApparatusRecipeInput input, Level level, BlockPos pos) {
        return biomeRequirement.matches(level, pos) && super.matches(input, level, input.player());
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return BiomeRecipeSerializers.APPARATUS.get();
    }

    public static final class Serializer implements RecipeSerializer<BiomeApparatusRecipe> {
        private static final MapCodec<BiomeApparatusRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                EnchantingApparatusRecipe.Serializer.CODEC.forGetter(recipe -> recipe),
                BiomeRequirement.CODEC.fieldOf("biomes").forGetter(BiomeApparatusRecipe::biomeRequirement)
        ).apply(instance, BiomeApparatusRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BiomeApparatusRecipe> STREAM_CODEC = StreamCodec.composite(
                EnchantingApparatusRecipe.Serializer.STREAM_CODEC, recipe -> recipe,
                BiomeRequirement.STREAM_CODEC, BiomeApparatusRecipe::biomeRequirement,
                BiomeApparatusRecipe::new
        );

        @Override
        public @NotNull MapCodec<BiomeApparatusRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, BiomeApparatusRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
