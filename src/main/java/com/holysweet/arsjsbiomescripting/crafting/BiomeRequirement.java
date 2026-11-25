package com.holysweet.arsjsbiomescripting.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class BiomeRequirement {
    public static final Codec<BiomeRequirement> CODEC = RegistryFixedCodec.create(Registries.BIOME).listOf()
            .validate(values -> values.isEmpty()
                    ? DataResult.error(() -> "biomes must contain at least one biome ID")
                    : DataResult.success(values))
            .xmap(BiomeRequirement::new, requirement -> requirement.biomes);
    public static final StreamCodec<RegistryFriendlyByteBuf, BiomeRequirement> STREAM_CODEC =
            ByteBufCodecs.holderRegistry(Registries.BIOME).apply(ByteBufCodecs.list())
                    .map(BiomeRequirement::new, requirement -> requirement.biomes);

    private final List<Holder<Biome>> biomes;
    private final Set<ResourceKey<Biome>> keys;

    private BiomeRequirement(List<Holder<Biome>> biomes) {
        this.biomes = List.copyOf(biomes);
        keys = biomes.stream().map(biome -> biome.unwrapKey().orElseThrow()).collect(Collectors.toUnmodifiableSet());
    }

    public Set<ResourceKey<Biome>> keys() {
        return keys;
    }

    public boolean matches(Level level, BlockPos pos) {
        return keys.stream().anyMatch(level.getBiome(pos)::is);
    }
}
