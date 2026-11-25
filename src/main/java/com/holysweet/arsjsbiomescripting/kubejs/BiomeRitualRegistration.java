package com.holysweet.arsjsbiomescripting.kubejs;

import com.holysweet.arsjsbiomescripting.ritual.BiomeRitualDefinition;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BiomeRitualRegistration implements KubeEvent {
    private final Registry<Biome> biomes;
    private final List<BiomeRitualDefinition> definitions = new ArrayList<>();

    BiomeRitualRegistration(RegistryAccess registries) {
        biomes = registries.registryOrThrow(Registries.BIOME);
    }

    List<BiomeRitualDefinition> definitions() {
        return List.copyOf(definitions);
    }

    @SuppressWarnings("unused") // Called from KubeJS scripts.
    public void ritual(String id, Map<String, Object> options) {
        try {
            if (options == null) {
                throw invalid("options", null, "expected a definition object");
            }
            Object value = options.getOrDefault("defaultMode", "Islands");
            String mode = requiredText("defaultMode", value);
            switch (mode) {
                case "BiomeOnly" -> define(id, options);
                case "Islands", "Replace", "ReplaceAndFill" -> throw invalid("defaultMode", mode,
                        "mode is not available yet; set defaultMode to BiomeOnly");
                default -> throw invalid("defaultMode", mode, "unknown ritual mode; use BiomeOnly in this version");
            }
        } catch (IllegalArgumentException exception) {
            reportError(id, exception);
        }
    }

    @SuppressWarnings("unused") // Retained for existing KubeJS scripts.
    public void biomeOnly(String id, Map<String, Object> options) {
        try {
            define(id, options);
        } catch (IllegalArgumentException exception) {
            reportError(id, exception);
        }
    }

    private static void reportError(String id, IllegalArgumentException exception) {
        ConsoleJS.SERVER.error("Biome ritual '" + id + "': " + exception.getMessage());
    }

    private void define(String id, Map<String, Object> options) {
        var ritualId = resourceId("id", id);
        if (options == null) {
            throw invalid("options", null, "expected a definition object");
        }
        var itemId = resourceId("input", options.get("input"));
        var item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            throw invalid("input", itemId, "expected a registered, non-air item");
        }
        var definition = new BiomeRitualDefinition(
                ritualId, requiredText("name", options.get("name")),
                requiredText("description", options.get("description")), item,
                startingBiomes(options.get("startingBiomes")), biome("targetBiome", options.get("targetBiome")),
                integer("radius", options.getOrDefault("radius", 7), 1, 64),
                integer("sourceCostPerBlock", options.getOrDefault("sourceCostPerBlock", 10), 0, Integer.MAX_VALUE / 5)
        );
        for (var existing : definitions) {
            if (existing.id().equals(ritualId)) {
                throw invalid("id", ritualId, "already registered");
            }
            if (definition.overlaps(existing)) {
                throw invalid("startingBiomes", options.get("startingBiomes"),
                        "overlaps ritual '" + existing.id() + "' for input '" + itemId + "'");
            }
        }
        definitions.add(definition);
    }

    private Set<ResourceKey<Biome>> startingBiomes(Object value) {
        if (value == null) {
            return Set.of();
        }
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            throw invalid("startingBiomes", value, "expected a non-empty array of biome IDs or omit the field");
        }
        Set<ResourceKey<Biome>> keys = new HashSet<>();
        for (Object element : list) {
            keys.add(biome("startingBiomes", element));
        }
        return Set.copyOf(keys);
    }

    private ResourceKey<Biome> biome(String field, Object value) {
        var id = resourceId(field, value);
        if (!biomes.containsKey(id)) {
            throw invalid(field, value, "unknown biome");
        }
        return ResourceKey.create(Registries.BIOME, id);
    }

    private static ResourceLocation resourceId(String field, Object value) {
        var id = value instanceof CharSequence text ? ResourceLocation.tryParse(text.toString()) : null;
        if (id == null || id.getPath().isEmpty()) {
            throw invalid(field, value, "expected a resource ID");
        }
        return id;
    }

    private static String requiredText(String field, Object value) {
        if (!(value instanceof CharSequence text) || text.toString().isBlank()) {
            throw invalid(field, value, "expected non-empty text");
        }
        return text.toString();
    }

    private static int integer(String field, Object value, int minimum, int maximum) {
        if (value instanceof Number number) {
            double result = number.doubleValue();
            if (result >= minimum && result <= maximum && result == Math.rint(result)) {
                return (int) result;
            }
        }
        throw invalid(field, value, "expected an integer from " + minimum + " through " + maximum);
    }

    private static IllegalArgumentException invalid(String field, Object value, String reason) {
        return new IllegalArgumentException("field '" + field + "' has value '" + value + "': " + reason);
    }
}
