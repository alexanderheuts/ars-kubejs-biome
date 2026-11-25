package com.holysweet.arsjsbiomescripting.ritual;

import com.hollingsworth.arsnouveau.common.block.RitualBrazierBlock;
import com.hollingsworth.arsnouveau.common.block.tile.RitualBrazierTile;
import com.holysweet.arsjsbiomescripting.ArsNouveauKubeJSBiomeScripting;
import com.holysweet.arsjsbiomescripting.kubejs.BiomeRitualKubePlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = ArsNouveauKubeJSBiomeScripting.MODID)
public final class BiomeRituals {
    private static List<BiomeRitualDefinition> definitions = List.of();

    private BiomeRituals() {}

    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent event) {
        reload(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            reload(event.getPlayerList().getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        definitions = List.of();
    }

    private static void reload(MinecraftServer server) {
        definitions = BiomeRitualKubePlugin.loadDefinitions(server.registryAccess());
    }

    @Nullable
    static BiomeRitualDefinition match(Item item, Holder<Biome> biome) {
        for (BiomeRitualDefinition definition : definitions) {
            if (definition.input() == item && definition.accepts(biome)) {
                return definition;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onBrazierClick(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        if (level.isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || event.getItemStack().isEmpty()) {
            return;
        }

        var brazier = brazierAt(level, event.getPos());
        var result = brazier == null ? null : offer(level, brazier, event.getEntity(), event.getItemStack());
        if (result != null) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent
    public static void onBucketUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getItemStack().getItem() instanceof BucketItem)
                || !(player.pick(player.blockInteractionRange(), 1.0F, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        var level = player.level();
        var pos = hit.getBlockPos();
        var brazier = brazierAt(level, pos);
        if (brazier == null) {
            return;
        }

        var stack = event.getItemStack();
        // The client's follow-up use packet can arrive after the block click already loaded the bucket.
        var result = brazier.ritual instanceof ConfiguredBiomeRitual ritual && ritual.hasStagedInput(stack)
                ? InteractionResult.FAIL : offer(level, brazier, player, stack);
        if (result != null) {
            event.setCanceled(true);
            event.setCancellationResult(result);
            player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
            player.connection.send(new ClientboundBlockUpdatePacket(level, pos.relative(hit.getDirection())));
        }
    }

    @Nullable
    private static RitualBrazierTile brazierAt(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof RitualBrazierBlock
                && level.getBlockEntity(pos) instanceof RitualBrazierTile brazier ? brazier : null;
    }

    @Nullable
    private static InteractionResult offer(Level level, RitualBrazierTile brazier, Player player, ItemStack stack) {
        if (brazier.ritual != null) {
            if (brazier.ritual instanceof ConfiguredBiomeRitual ritual && ritual.isAugment(stack)) {
                return brazier.tryBurnStack(stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
            return null;
        }

        Item item = stack.getItem();
        if (definitions.stream().noneMatch(definition -> definition.input() == item)) {
            return null;
        }

        BiomeRitualDefinition definition = match(item, level.getBiome(brazier.getBlockPos()));
        if (definition == null) {
            return InteractionResult.FAIL;
        }

        brazier.setRitual(ConfiguredBiomeRitual.ID);
        if (brazier.ritual instanceof ConfiguredBiomeRitual ritual) {
            boolean consume = !player.hasInfiniteMaterials();
            ritual.stage(definition, consume ? stack.split(1) : stack.copyWithCount(1), consume);
            brazier.updateBlock();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
