package com.holysweet.arsjsbiomescripting.kubejs;

import com.holysweet.arsjsbiomescripting.ritual.BiomeRitualDefinition;
import com.holysweet.arsjsbiomescripting.ritual.BiomeRitualMode;
import com.holysweet.arsjsbiomescripting.ritual.AugmentOverrides;
import com.holysweet.arsjsbiomescripting.ritual.LayerOverrides;
import com.holysweet.arsjsbiomescripting.ritual.RadiusAugment;
import com.holysweet.arsjsbiomescripting.ritual.RitualAugment;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
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
            define(id, options, mode("defaultMode", options.getOrDefault("defaultMode", "Islands")));
        } catch (IllegalArgumentException exception) {
            reportError(id, exception);
        }
    }

    @SuppressWarnings("unused") // Retained for existing KubeJS scripts.
    public void biomeOnly(String id, Map<String, Object> options) {
        try {
            define(id, options, BiomeRitualMode.BIOME_ONLY);
        } catch (IllegalArgumentException exception) {
            reportError(id, exception);
        }
    }

    private static void reportError(String id, IllegalArgumentException exception) {
        ConsoleJS.SERVER.error("Biome ritual '" + id + "': " + exception.getMessage());
    }

    private static BiomeRitualMode mode(String field, Object value) {
        return switch (requiredText(field, value)) {
            case "BiomeOnly" -> BiomeRitualMode.BIOME_ONLY;
            case "Islands" -> BiomeRitualMode.ISLANDS;
            case "Replace" -> BiomeRitualMode.REPLACE;
            case "ReplaceAndFill" -> BiomeRitualMode.REPLACE_AND_FILL;
            default -> throw invalid(field, value, "expected BiomeOnly, Islands, Replace, or ReplaceAndFill");
        };
    }

    private void define(String id, Map<String, Object> options, BiomeRitualMode mode) {
        var ritualId = resourceId("id", id);
        if (options == null) {
            throw invalid("options", null, "expected a definition object");
        }
        Item item = item("input", options.get("input"));
        LayerOverrides blocks = options.containsKey("blocks")
                ? layerOverrides("blocks", options.get("blocks")) : LayerOverrides.EMPTY;
        if (mode != BiomeRitualMode.BIOME_ONLY && blocks.layer1() == null) {
            throw invalid("blocks.layer1", null, "required for a block-placing mode");
        }
        var definition = new BiomeRitualDefinition(
                ritualId, requiredText("name", options.get("name")),
                requiredText("description", options.get("description")), item,
                startingBiomes(options.get("startingBiomes")), biome("targetBiome", options.get("targetBiome")),
                integer("radius", options.getOrDefault("radius", 7), 1, 64),
                integer("sourceCostPerBlock", options.getOrDefault("sourceCostPerBlock", 10), 0, Integer.MAX_VALUE / 5),
                mode, blocks, radiusAugment(options.get("radiusAugment")), augments(options.get("augments"), blocks)
        );
        for (var existing : definitions) {
            if (existing.id().equals(ritualId)) {
                throw invalid("id", ritualId, "already registered");
            }
            if (definition.overlaps(existing)) {
                throw invalid("startingBiomes", options.get("startingBiomes"),
                        "overlaps ritual '" + existing.id() + "' for input '" + BuiltInRegistries.ITEM.getKey(item) + "'");
            }
        }
        definitions.add(definition);
    }

    @Nullable
    private static RadiusAugment radiusAugment(Object value) {
        if (Boolean.FALSE.equals(value)) {
            return null;
        }
        Map<?, ?> options;
        if (value == null) {
            options = Map.of();
        } else if (value instanceof Map<?, ?> map) {
            options = map;
        } else {
            throw invalid("radiusAugment", value, "expected an options object or false");
        }
        Item item = item("radiusAugment.item",
                options.containsKey("item") ? options.get("item") : "ars_nouveau:source_gem");
        int increase = integer("radiusAugment.increase", options.containsKey("increase") ? options.get("increase") : 1,
                1, RadiusAugment.MAX_ADDED_RADIUS);
        int maxCount = integer("radiusAugment.maxCount", options.containsKey("maxCount") ? options.get("maxCount") : 64,
                1, RadiusAugment.MAX_ADDED_RADIUS / increase);
        return new RadiusAugment(item, increase, maxCount);
    }

    private static Item item(String field, Object value) {
        var id = resourceId(field, value);
        var item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            throw invalid(field, value, "expected a registered, non-air item");
        }
        return item;
    }

    private Map<Item, RitualAugment> augments(Object value, LayerOverrides baseBlocks) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof List<?> list)) {
            throw invalid("augments", value, "expected an array of augment definitions");
        }
        Map<Item, RitualAugment> result = new HashMap<>();
        for (int index = 0; index < list.size(); index++) {
            String field = "augments[" + index + "]";
            if (!(list.get(index) instanceof Map<?, ?> entry)) {
                throw invalid(field, list.get(index), "expected an augment object");
            }
            Item item = item(field + ".item", entry.get("item"));
            int maxCount = entry.containsKey("maxCount")
                    ? integer(field + ".maxCount", entry.get("maxCount"), 1, Integer.MAX_VALUE) : 1;
            AugmentOverrides overrides = augmentOverrides(field + ".overrides", entry.get("overrides"));
            if (overrides.mode() != null && overrides.mode() != BiomeRitualMode.BIOME_ONLY
                    && baseBlocks.then(overrides.blocks()).layer1() == null) {
                throw invalid(field + ".overrides.blocks.layer1", null,
                        "required when switching to a block-placing mode without a base layer1");
            }
            if (result.putIfAbsent(item, new RitualAugment(maxCount, overrides)) != null) {
                throw invalid(field + ".item", entry.get("item"), "already used by another custom augment");
            }
        }
        return Map.copyOf(result);
    }

    private AugmentOverrides augmentOverrides(String field, Object value) {
        if (!(value instanceof Map<?, ?> values) || values.isEmpty()) {
            throw invalid(field, value, "expected a non-empty overrides object");
        }
        for (Object key : values.keySet()) {
            if (!Set.of("targetBiome", "radius", "sourceCostPerBlock", "mode", "blocks").contains(key)) {
                throw invalid(field + "." + key, values.get(key), "unknown override");
            }
        }
        return new AugmentOverrides(
                values.containsKey("targetBiome") ? biome(field + ".targetBiome", values.get("targetBiome")) : null,
                values.containsKey("radius") ? integer(field + ".radius", values.get("radius"), 1, 64) : null,
                values.containsKey("sourceCostPerBlock")
                        ? integer(field + ".sourceCostPerBlock", values.get("sourceCostPerBlock"), 0, Integer.MAX_VALUE / 5)
                        : null,
                values.containsKey("mode") ? mode(field + ".mode", values.get("mode")) : null,
                values.containsKey("blocks") ? layerOverrides(field + ".blocks", values.get("blocks")) : LayerOverrides.EMPTY
        );
    }

    private static LayerOverrides layerOverrides(String field, Object value) {
        if (!(value instanceof Map<?, ?> blocks)) {
            throw invalid(field, value, "expected an object containing layer block IDs");
        }
        for (Object key : blocks.keySet()) {
            if (!Set.of("layer1", "layer2", "layer3To4", "layer5Plus").contains(key)) {
                throw invalid(field + "." + key, blocks.get(key), "unknown block layer");
            }
        }
        return new LayerOverrides(
                optionalBlock(blocks, field, "layer1"), optionalBlock(blocks, field, "layer2"),
                optionalBlock(blocks, field, "layer3To4"), optionalBlock(blocks, field, "layer5Plus")
        );
    }

    @Nullable
    private static Block optionalBlock(Map<?, ?> blocks, String path, String field) {
        return blocks.containsKey(field) ? block(path + "." + field, blocks.get(field)) : null;
    }

    private static Block block(String field, Object value) {
        var id = resourceId(field, value);
        var block = BuiltInRegistries.BLOCK.get(id);
        if (block.defaultBlockState().isAir()) {
            throw invalid(field, value, "expected a registered, non-air block");
        }
        return block;
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
