package com.holysweet.arsjsbiomescripting.ritual;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public record BiomeRitualDefinition(
        ResourceLocation id, String name, String description, Item input,
        Set<ResourceKey<Biome>> startingBiomes, @Nullable ResourceKey<Biome> targetBiome,
        int radius, int sourceCostPerBlock, BiomeRitualMode mode, LayerOverrides blocks,
        @Nullable RadiusAugment radiusAugment, Map<Item, RitualAugment> augments
) {
    public boolean accepts(Holder<Biome> biome) {
        return startingBiomes.isEmpty() || startingBiomes.stream().anyMatch(biome::is);
    }

    public boolean overlaps(BiomeRitualDefinition other) {
        return input == other.input && (startingBiomes.isEmpty() || other.startingBiomes.isEmpty()
                || startingBiomes.stream().anyMatch(other.startingBiomes::contains));
    }
}
