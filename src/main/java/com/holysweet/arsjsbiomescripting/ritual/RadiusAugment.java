package com.holysweet.arsjsbiomescripting.ritual;

import net.minecraft.world.item.Item;

public record RadiusAugment(Item item, int increase, int maxCount) {
    // The Ars tracker adds both horizontal bounds and the vertical bound into an int.
    public static final int MAX_ADDED_RADIUS = (Integer.MAX_VALUE - 3) / 2 - 64;
}
