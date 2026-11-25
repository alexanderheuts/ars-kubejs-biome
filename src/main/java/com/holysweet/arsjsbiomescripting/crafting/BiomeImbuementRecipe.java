package com.holysweet.arsjsbiomescripting.crafting;

import com.hollingsworth.arsnouveau.common.block.tile.ImbuementTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class BiomeImbuementRecipe extends ImbuementRecipe {
    private final List<Holder<Biome>> biomes;
    private final Set<ResourceKey<Biome>> biomeKeys;

    private BiomeImbuementRecipe(ImbuementRecipe recipe, List<Holder<Biome>> biomes) {
        super(recipe.getInput(), recipe.getOutput(), recipe.getSource(), recipe.getPedestalItems());
        this.biomes = List.copyOf(biomes);
        biomeKeys = biomes.stream().map(biome -> biome.unwrapKey().orElseThrow()).collect(Collectors.toUnmodifiableSet());
    }

    public Set<ResourceKey<Biome>> biomeKeys() {
        return biomeKeys;
    }

    @Override
    public boolean matches(ImbuementTile chamber, Level level) {
        return biomeKeys.stream().anyMatch(level.getBiome(chamber.getBlockPos())::is)
                && super.matches(chamber, level);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return BiomeRecipeSerializers.IMBUEMENT.get();
    }

    public static final class Serializer implements RecipeSerializer<BiomeImbuementRecipe> {
        private static final MapCodec<BiomeImbuementRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ImbuementRecipe.Serializer.CODEC.forGetter(recipe -> recipe),
                RegistryFixedCodec.create(Registries.BIOME).listOf()
                        .validate(values -> values.isEmpty()
                                ? DataResult.error(() -> "biomes must contain at least one biome ID")
                                : DataResult.success(values))
                        .fieldOf("biomes").forGetter(recipe -> recipe.biomes)
        ).apply(instance, BiomeImbuementRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BiomeImbuementRecipe> STREAM_CODEC = StreamCodec.composite(
                ImbuementRecipe.Serializer.STREAM_CODEC, recipe -> recipe,
                ByteBufCodecs.holderRegistry(Registries.BIOME).apply(ByteBufCodecs.list()), recipe -> recipe.biomes,
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
