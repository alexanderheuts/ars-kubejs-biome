package com.holysweet.arsjsbiomescripting.ritual;

public enum BiomeRitualMode {
    BIOME_ONLY,
    ISLANDS,
    REPLACE;

    boolean accepts(boolean air) {
        return switch (this) {
            case BIOME_ONLY -> true;
            case ISLANDS -> air;
            case REPLACE -> !air;
        };
    }
}
