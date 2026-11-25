package com.holysweet.arsjsbiomescripting.ritual;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public record IslandBlocks(Block layer1, Block layer2, Block layer3To4, Block layer5Plus) {
    public Block atDepth(int depth) {
        return switch (depth) {
            case 1 -> layer1;
            case 2 -> layer2;
            case 3, 4 -> layer3To4;
            default -> layer5Plus;
        };
    }

    boolean isValid() {
        return !layer1.defaultBlockState().isAir() && !layer2.defaultBlockState().isAir()
                && !layer3To4.defaultBlockState().isAir() && !layer5Plus.defaultBlockState().isAir();
    }

    CompoundTag save() {
        var tag = new CompoundTag();
        tag.putString("layer1", BuiltInRegistries.BLOCK.getKey(layer1).toString());
        tag.putString("layer2", BuiltInRegistries.BLOCK.getKey(layer2).toString());
        tag.putString("layer3To4", BuiltInRegistries.BLOCK.getKey(layer3To4).toString());
        tag.putString("layer5Plus", BuiltInRegistries.BLOCK.getKey(layer5Plus).toString());
        return tag;
    }

    static IslandBlocks load(CompoundTag tag) {
        return new IslandBlocks(readBlock(tag, "layer1"), readBlock(tag, "layer2"),
                readBlock(tag, "layer3To4"), readBlock(tag, "layer5Plus"));
    }

    private static Block readBlock(CompoundTag tag, String field) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(field));
        return id == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(id);
    }
}
