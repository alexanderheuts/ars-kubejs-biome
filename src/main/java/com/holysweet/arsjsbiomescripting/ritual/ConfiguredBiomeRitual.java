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
    private static final int MAX_POSITIONS_SCANNED_PER_TICK = 4096;
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
    private int radiusBonus;
    private AugmentOverrides stagedOverrides = AugmentOverrides.EMPTY;
    private int sourceCostPerBlock;
    private BiomeRitualMode mode = BiomeRitualMode.BIOME_ONLY;
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
        targetBiome = stagedOverrides.targetBiome() != null ? stagedOverrides.targetBiome() : definition.targetBiome();
        radius = (stagedOverrides.radius() != null ? stagedOverrides.radius() : definition.radius()) + radiusBonus;
        sourceCostPerBlock = stagedOverrides.sourceCostPerBlock() != null
                ? stagedOverrides.sourceCostPerBlock() : definition.sourceCostPerBlock();
        mode = stagedOverrides.mode() != null ? stagedOverrides.mode() : definition.mode();
        islandBlocks = mode == BiomeRitualMode.BIOME_ONLY ? null
                : definition.blocks().then(stagedOverrides.blocks()).resolve();
    }

    @Nullable
    private BiomeRitualDefinition currentDefinition(Level level, BlockPos position) {
        return BiomeRituals.match(input.getItem(), level.getBiome(position));
    }

    @Nullable
    private BiomeRitualDefinition stagedDefinition() {
        Level level = getWorld();
        BlockPos pos = getPos();
        if (input.isEmpty() || level == null || pos == null || isRunning() || isDone()) {
            return null;
        }
        return currentDefinition(level, pos);
    }

    boolean isAugment(ItemStack stack) {
        var definition = stagedDefinition();
        return definition != null && (definition.augments().containsKey(stack.getItem())
                || (definition.radiusAugment() != null && stack.is(definition.radiusAugment().item())));
    }

    @Override
    public boolean canConsumeItem(ItemStack stack) {
        var definition = stagedDefinition();
        return definition != null && canAcceptAugment(definition, stack);
    }

    private boolean canAcceptAugment(BiomeRitualDefinition definition, ItemStack stack) {
        var item = stack.getItem();
        int count = itemConsumedCount(consumed -> consumed.is(item));
        var custom = definition.augments().get(item);
        if (custom != null) {
            return count < custom.maxCount();
        }
        var radiusAugment = definition.radiusAugment();
        return radiusAugment != null && stack.is(radiusAugment.item()) && count < radiusAugment.maxCount()
                && radiusBonus <= RadiusAugment.MAX_ADDED_RADIUS - radiusAugment.increase();
    }

    @Override
    public void onItemConsumed(ItemStack stack) {
        var definition = stagedDefinition();
        if (definition == null || !canAcceptAugment(definition, stack)) {
            return;
        }
        var custom = definition.augments().get(stack.getItem());
        if (custom != null) {
            stagedOverrides = stagedOverrides.then(custom.overrides());
            if (custom.overrides().radius() != null) {
                radiusBonus = 0;
            }
        } else {
            var radiusAugment = definition.radiusAugment();
            if (radiusAugment != null) {
                radiusBonus += radiusAugment.increase();
            }
        }
        resolve(definition);
        super.onItemConsumed(stack);
        tile.setChanged();
    }

    @Override
    public boolean canStart(@Nullable Player player) {
        Level level = getWorld();
        BlockPos position = getPos();
        return !input.isEmpty() && level != null && position != null
                && (level.isClientSide() || canResolve(currentDefinition(level, position)));
    }

    private boolean canResolve(@Nullable BiomeRitualDefinition definition) {
        if (definition == null) {
            return false;
        }
        var resolvedMode = stagedOverrides.mode() != null ? stagedOverrides.mode() : definition.mode();
        return resolvedMode == BiomeRitualMode.BIOME_ONLY
                || definition.blocks().then(stagedOverrides.blocks()).layer1() != null;
    }

    @Override
    public void onStart(@Nullable Player player) {
        BlockPos position = getPos();
        if (!(getWorld() instanceof ServerLevel level) || position == null) {
            return;
        }
        var definition = currentDefinition(level, position);
        if (definition == null || !canResolve(definition)) {
            return;
        }
        resolve(definition);
        radiusBonus = 0;
        stagedOverrides = AugmentOverrides.EMPTY;
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
                var current = level.getBlockState(pos);
                var replacement = islandBlocks.atDepth(depth).defaultBlockState();
                if (!mode.accepts(current.isAir())
                        || (current != replacement && !level.setBlock(pos, replacement, Block.UPDATE_ALL))) {
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
        boolean exhausted = false;
        for (int scanned = 0; count < BATCH_SIZE && scanned < MAX_POSITIONS_SCANNED_PER_TICK; scanned++) {
            BlockPos next = tracker.computeNext();
            if (next == null) {
                exhausted = true;
                break;
            }
            int depth = origin.getY() - next.getY();
            if (depth >= 1 && depth <= MAX_DEPTH && level.isInWorldBounds(next)
                    && isInFootprint(next, depth) && acceptsPosition(level, next)) {
                positions[count++] = next.asLong();
            }
        }
        batch = Arrays.copyOf(positions, count);
        if (count == 0 && exhausted) {
            setFinished();
        } else {
            boolean needsSource = getSourceCost() > 0;
            if (needsSourceNow() != needsSource) {
                setNeedsSource(needsSource);
            }
        }
        markProgress(level);
    }

    private boolean acceptsPosition(ServerLevel level, BlockPos pos) {
        return mode == BiomeRitualMode.BIOME_ONLY || mode == BiomeRitualMode.REPLACE_AND_FILL
                || mode.accepts(level.isEmptyBlock(pos));
    }

    private boolean isInFootprint(BlockPos pos, int depth) {
        if (islandBlocks == null) {
            return pos.distSqr(origin) <= (double) radius * radius;
        }
        // Scale the width of each layer while keeping the depth fixed.
        double dx = (double) pos.getX() - origin.getX();
        double dz = (double) pos.getZ() - origin.getZ();
        int depthSquared = MAX_DEPTH * MAX_DEPTH;
        return (dx * dx + dz * dz) * depthSquared <= (double) radius * radius * (depthSquared - depth * depth);
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
        if ((mode == BiomeRitualMode.ISLANDS || mode == BiomeRitualMode.REPLACE)
                && getWorld() instanceof ServerLevel level) {
            count = 0;
            for (long position : batch) {
                if (acceptsPosition(level, BlockPos.of(position))) {
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
            state.putString("mode", mode.name());
        }
        if (!input.isEmpty()) {
            state.put("input", input.save(registries));
            state.putBoolean("refundableInput", refundableInput);
            state.putInt("radiusBonus", radiusBonus);
            state.put("augmentOverrides", stagedOverrides.save());
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
        // Saves before mode was recorded already distinguish BiomeOnly and Islands by their block layers.
        mode = islandBlocks == null ? BiomeRitualMode.BIOME_ONLY : switch (state.getString("mode")) {
            case "REPLACE" -> BiomeRitualMode.REPLACE;
            case "REPLACE_AND_FILL" -> BiomeRitualMode.REPLACE_AND_FILL;
            default -> BiomeRitualMode.ISLANDS;
        };
        refundableInput = state.getBoolean("refundableInput");
        radiusBonus = state.getInt("radiusBonus");
        stagedOverrides = AugmentOverrides.load(state.getCompound("augmentOverrides"));
        if (state.contains("tracker", Tag.TAG_COMPOUND)) {
            origin = BlockPos.of(state.getLong("origin"));
            tracker = new ManhattenTracker(state.getCompound("tracker"));
            batch = state.getLongArray("batch");
        }
    }
}
