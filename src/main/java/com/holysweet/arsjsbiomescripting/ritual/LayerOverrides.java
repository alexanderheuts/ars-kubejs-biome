package com.holysweet.arsjsbiomescripting.ritual;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public record LayerOverrides(
        @Nullable Block layer1, @Nullable Block layer2,
        @Nullable Block layer3To4, @Nullable Block layer5Plus
) {
    public static final LayerOverrides EMPTY = new LayerOverrides(null, null, null, null);

    public LayerOverrides then(LayerOverrides later) {
        return new LayerOverrides(
                later.layer1 != null ? later.layer1 : layer1,
                later.layer2 != null ? later.layer2 : layer2,
                later.layer3To4 != null ? later.layer3To4 : layer3To4,
                later.layer5Plus != null ? later.layer5Plus : layer5Plus
        );
    }

    @Nullable
    IslandBlocks resolve() {
        if (layer1 == null) {
            return null;
        }
        Block second = layer2 != null ? layer2 : layer1;
        Block middle = layer3To4 != null ? layer3To4 : second;
        Block bottom = layer5Plus != null ? layer5Plus : middle;
        return new IslandBlocks(layer1, second, middle, bottom);
    }

    CompoundTag save() {
        var tag = new CompoundTag();
        saveBlock(tag, "layer1", layer1);
        saveBlock(tag, "layer2", layer2);
        saveBlock(tag, "layer3To4", layer3To4);
        saveBlock(tag, "layer5Plus", layer5Plus);
        return tag;
    }

    static LayerOverrides load(CompoundTag tag) {
        return new LayerOverrides(readBlock(tag, "layer1"), readBlock(tag, "layer2"),
                readBlock(tag, "layer3To4"), readBlock(tag, "layer5Plus"));
    }

    private static void saveBlock(CompoundTag tag, String field, @Nullable Block block) {
        if (block != null) {
            tag.putString(field, BuiltInRegistries.BLOCK.getKey(block).toString());
        }
    }

    @Nullable
    private static Block readBlock(CompoundTag tag, String field) {
        var id = ResourceLocation.tryParse(tag.getString(field));
        return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }
}
