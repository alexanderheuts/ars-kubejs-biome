package com.holysweet.arsjsbiomescripting.ritual;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

public record AugmentOverrides(
        @Nullable ResourceKey<Biome> targetBiome, @Nullable Integer radius,
        @Nullable Integer sourceCostPerBlock, @Nullable BiomeRitualMode mode, LayerOverrides blocks
) {
    static final AugmentOverrides EMPTY = new AugmentOverrides(null, null, null, null, LayerOverrides.EMPTY);

    AugmentOverrides then(AugmentOverrides later) {
        return new AugmentOverrides(
                later.targetBiome != null ? later.targetBiome : targetBiome,
                later.radius != null ? later.radius : radius,
                later.sourceCostPerBlock != null ? later.sourceCostPerBlock : sourceCostPerBlock,
                later.mode != null ? later.mode : mode,
                blocks.then(later.blocks)
        );
    }

    CompoundTag save() {
        var tag = new CompoundTag();
        if (targetBiome != null) tag.putString("targetBiome", targetBiome.location().toString());
        if (radius != null) tag.putInt("radius", radius);
        if (sourceCostPerBlock != null) tag.putInt("sourceCostPerBlock", sourceCostPerBlock);
        if (mode != null) tag.putString("mode", mode.name());
        tag.put("blocks", blocks.save());
        return tag;
    }

    static AugmentOverrides load(CompoundTag tag) {
        var biomeId = tag.contains("targetBiome", Tag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString("targetBiome")) : null;
        return new AugmentOverrides(
                biomeId == null ? null : ResourceKey.create(Registries.BIOME, biomeId),
                tag.contains("radius", Tag.TAG_INT) ? tag.getInt("radius") : null,
                tag.contains("sourceCostPerBlock", Tag.TAG_INT) ? tag.getInt("sourceCostPerBlock") : null,
                switch (tag.getString("mode")) {
                    case "BIOME_ONLY" -> BiomeRitualMode.BIOME_ONLY;
                    case "ISLANDS" -> BiomeRitualMode.ISLANDS;
                    case "REPLACE" -> BiomeRitualMode.REPLACE;
                    case "REPLACE_AND_FILL" -> BiomeRitualMode.REPLACE_AND_FILL;
                    default -> null;
                },
                LayerOverrides.load(tag.getCompound("blocks"))
        );
    }
}
