package com.holysweet.arsjsbiomescripting.ritual;

public enum BiomeRitualMode {
    BIOME_ONLY,
    ISLANDS,
    REPLACE,
    REPLACE_AND_FILL;

    boolean accepts(boolean air) {
        return switch (this) {
            case BIOME_ONLY, REPLACE_AND_FILL -> true;
            case ISLANDS -> air;
            case REPLACE -> !air;
        };
    }
}
