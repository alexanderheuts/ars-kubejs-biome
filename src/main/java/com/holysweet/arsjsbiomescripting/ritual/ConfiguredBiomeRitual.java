package com.holysweet.arsjsbiomescripting.ritual;

import com.hollingsworth.arsnouveau.api.ritual.AbstractRitual;
import com.hollingsworth.arsnouveau.api.ritual.ManhattenTracker;
import com.hollingsworth.arsnouveau.api.ritual.RitualUtil;
import com.holysweet.arsjsbiomescripting.ArsNouveauKubeJSBiomeScripting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public final class ConfiguredBiomeRitual extends AbstractRitual {
    // Keep the registry ID used by saved biome-only rituals.
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            ArsNouveauKubeJSBiomeScripting.MODID, "biome_only"
    );
    private static final int BATCH_SIZE = 5;
    private static final int MAX_DEPTH = 7;
    private static final String STATE_TAG = "arsjsbiomescripting";

    private ResourceLocation definitionId;
    private String name = "Biome Ritual";
    private String description = "";
    private ItemStack input = ItemStack.EMPTY;
    private boolean refundableInput;
    private ResourceKey<Biome> targetBiome;
    private BlockPos origin;
    private int radius;
    private int sourceCostPerBlock;
    @Nullable
    private IslandBlocks islandBlocks;
    private ManhattenTracker tracker;
    private long[] batch = new long[0];

    public ConfiguredBiomeRitual() {}

    void stage(BiomeRitualDefinition definition, ItemStack offeredItem, boolean refundable) {
        input = offeredItem;
        refundableInput = refundable;
        resolve(definition);
    }

    private void resolve(BiomeRitualDefinition definition) {
        definitionId = definition.id();
        name = definition.name();
        description = definition.description();
        targetBiome = definition.targetBiome();
        radius = definition.radius();
        sourceCostPerBlock = definition.sourceCostPerBlock();
        islandBlocks = definition.islandBlocks();
    }

    @Nullable
    private BiomeRitualDefinition currentDefinition(Level level, BlockPos position) {
        return BiomeRituals.match(input.getItem(), level.getBiome(position));
    }

    @Override
    public boolean canStart(@Nullable Player player) {
        Level level = getWorld();
        BlockPos position = getPos();
        return !input.isEmpty() && level != null && position != null
                && (level.isClientSide() || currentDefinition(level, position) != null);
    }

    @Override
    public void onStart(@Nullable Player player) {
        BlockPos position = getPos();
        if (!(getWorld() instanceof ServerLevel level) || position == null) {
            return;
        }
        var definition = currentDefinition(level, position);
        if (definition == null) {
            return;
        }
        resolve(definition);
        origin = position.immutable();
        tracker = islandBlocks == null
                ? new ManhattenTracker(origin.below(3), radius, 2, radius)
                : new ManhattenTracker(origin.below(4), radius, 3, radius);
        input = ItemStack.EMPTY;
        refundableInput = false;
        super.onStart(player);
        queueBatch(level);
    }

    @Override
    protected void tick() {
        if (!(getWorld() instanceof ServerLevel level) || (needsSourceNow() && getSourceCost() > 0)) {
            return;
        }
        if (tracker == null || targetBiome == null || (islandBlocks != null && !islandBlocks.isValid())
                || !level.registryAccess().registryOrThrow(Registries.BIOME).containsKey(targetBiome.location())) {
            setFinished();
            markProgress(level);
            return;
        }
        for (long position : batch) {
            BlockPos pos = BlockPos.of(position);
            if (islandBlocks != null) {
                int depth = origin.getY() - pos.getY();
                if (!level.isEmptyBlock(pos)
                        || !level.setBlock(pos, islandBlocks.atDepth(depth).defaultBlockState(), Block.UPDATE_ALL)) {
                    continue;
                }
            }
            RitualUtil.changeBiome(level, pos, targetBiome);
        }
        queueBatch(level);
    }

    private void queueBatch(ServerLevel level) {
        long[] positions = new long[BATCH_SIZE];
        int count = 0;
        while (count < BATCH_SIZE) {
            BlockPos next = tracker.computeNext();
            if (next == null) {
                break;
            }
            int depth = origin.getY() - next.getY();
            if (depth >= 1 && depth <= MAX_DEPTH && !level.isOutsideBuildHeight(next)
                    && isInFootprint(next, depth) && (islandBlocks == null || level.isEmptyBlock(next))) {
                positions[count++] = next.asLong();
            }
        }
        batch = Arrays.copyOf(positions, count);
        if (count == 0) {
            setFinished();
        } else {
            setNeedsSource(getSourceCost() > 0);
        }
        markProgress(level);
    }

    private boolean isInFootprint(BlockPos pos, int depth) {
        if (islandBlocks == null) {
            return pos.distSqr(origin) <= (double) radius * radius;
        }
        // Scale the width of each layer while keeping the depth fixed.
        int dx = pos.getX() - origin.getX();
        int dz = pos.getZ() - origin.getZ();
        int depthSquared = MAX_DEPTH * MAX_DEPTH;
        return (dx * dx + dz * dz) * depthSquared <= radius * radius * (depthSquared - depth * depth);
    }

    private void markProgress(ServerLevel level) {
        if (origin != null && level.getBlockEntity(origin) != null) {
            level.blockEntityChanged(origin);
        }
    }

    public ItemStack removeStagedInput() {
        ItemStack staged = refundableInput ? input : ItemStack.EMPTY;
        input = ItemStack.EMPTY;
        refundableInput = false;
        return staged;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void modifyTooltips(List<Component> tooltips) {
        if (!description.isEmpty()) {
            tooltips.add(Component.literal(description));
        }
    }

    @Override
    public int getSourceCost() {
        int count = batch.length;
        if (islandBlocks != null && getWorld() instanceof ServerLevel level) {
            count = 0;
            for (long position : batch) {
                if (level.isEmptyBlock(BlockPos.of(position))) {
                    count++;
                }
            }
        }
        return count * sourceCostPerBlock;
    }

    @Override
    public void write(HolderLookup.Provider registries, CompoundTag tag) {
        super.write(registries, tag);
        CompoundTag state = new CompoundTag();
        if (definitionId != null && targetBiome != null) {
            state.putString("definition", definitionId.toString());
            state.putString("name", name);
            state.putString("description", description);
            state.putString("targetBiome", targetBiome.location().toString());
            state.putInt("radius", radius);
            state.putInt("sourceCost", sourceCostPerBlock);
        }
        if (!input.isEmpty()) {
            state.put("input", input.save(registries));
            state.putBoolean("refundableInput", refundableInput);
        }
        if (islandBlocks != null) {
            state.put("islandBlocks", islandBlocks.save());
        }
        if (tracker != null) {
            state.putLong("origin", origin.asLong());
            state.put("tracker", tracker.serialize(new CompoundTag()));
            state.putLongArray("batch", batch);
        }
        tag.put(STATE_TAG, state);
    }

    @Override
    public void read(HolderLookup.Provider registries, CompoundTag tag) {
        super.read(registries, tag);
        CompoundTag state = tag.getCompound(STATE_TAG);
        if (state.contains("definition", Tag.TAG_STRING)) {
            definitionId = ResourceLocation.tryParse(state.getString("definition"));
            name = state.getString("name");
            description = state.getString("description");
            ResourceLocation target = ResourceLocation.tryParse(state.getString("targetBiome"));
            targetBiome = target == null ? null : ResourceKey.create(Registries.BIOME, target);
            radius = state.getInt("radius");
            sourceCostPerBlock = state.getInt("sourceCost");
        }
        input = ItemStack.parseOptional(registries, state.getCompound("input"));
        islandBlocks = state.contains("islandBlocks", Tag.TAG_COMPOUND)
                ? IslandBlocks.load(state.getCompound("islandBlocks")) : null;
        refundableInput = state.getBoolean("refundableInput");
        if (state.contains("tracker", Tag.TAG_COMPOUND)) {
            origin = BlockPos.of(state.getLong("origin"));
            tracker = new ManhattenTracker(state.getCompound("tracker"));
            batch = state.getLongArray("batch");
        }
    }
}
