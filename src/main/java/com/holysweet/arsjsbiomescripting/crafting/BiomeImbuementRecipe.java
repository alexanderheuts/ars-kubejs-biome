package com.holysweet.arsjsbiomescripting.crafting;

import com.hollingsworth.arsnouveau.common.block.tile.ImbuementTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class BiomeImbuementRecipe extends ImbuementRecipe implements BiomeRestrictedRecipe {
    private final BiomeRequirement biomeRequirement;

    private BiomeImbuementRecipe(ImbuementRecipe recipe, BiomeRequirement biomeRequirement) {
        super(recipe.getInput(), recipe.getOutput(), recipe.getSource(), recipe.getPedestalItems());
        this.biomeRequirement = biomeRequirement;
    }

    @Override
    public BiomeRequirement biomeRequirement() {
        return biomeRequirement;
    }

    @Override
    public Ingredient primaryIngredient() {
        return getInput();
    }

    @Override
    public List<Ingredient> pedestalIngredients() {
        return getPedestalItems();
    }

    @Override
    public boolean matches(ImbuementTile chamber, Level level) {
        return biomeRequirement.matches(level, chamber.getBlockPos()) && super.matches(chamber, level);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return BiomeRecipeSerializers.IMBUEMENT.get();
    }

    public static final class Serializer implements RecipeSerializer<BiomeImbuementRecipe> {
        private static final MapCodec<BiomeImbuementRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ImbuementRecipe.Serializer.CODEC.forGetter(recipe -> recipe),
                BiomeRequirement.CODEC.fieldOf("biomes").forGetter(BiomeImbuementRecipe::biomeRequirement)
        ).apply(instance, BiomeImbuementRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BiomeImbuementRecipe> STREAM_CODEC = StreamCodec.composite(
                ImbuementRecipe.Serializer.STREAM_CODEC, recipe -> recipe,
                BiomeRequirement.STREAM_CODEC, BiomeImbuementRecipe::biomeRequirement,
                BiomeImbuementRecipe::new
        );

        @Override
        public @NotNull MapCodec<BiomeImbuementRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, BiomeImbuementRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
