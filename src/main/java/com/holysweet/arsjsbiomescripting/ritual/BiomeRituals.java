package com.holysweet.arsjsbiomescripting.ritual;

import com.hollingsworth.arsnouveau.common.block.RitualBrazierBlock;
import com.hollingsworth.arsnouveau.common.block.tile.RitualBrazierTile;
import com.holysweet.arsjsbiomescripting.ArsNouveauKubeJSBiomeScripting;
import com.holysweet.arsjsbiomescripting.kubejs.BiomeRitualKubePlugin;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
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
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || event.getItemStack().isEmpty()
                || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof RitualBrazierBlock)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof RitualBrazierTile brazier)
                || brazier.ritual != null) {
            return;
        }

        Item item = event.getItemStack().getItem();
        if (definitions.stream().noneMatch(definition -> definition.input() == item)) {
            return;
        }

        BiomeRitualDefinition definition = match(item, event.getLevel().getBiome(event.getPos()));
        event.setCanceled(true);
        event.setCancellationResult(definition == null ? InteractionResult.FAIL : InteractionResult.SUCCESS);
        if (definition == null) {
            return;
        }

        brazier.setRitual(BiomeOnlyRitual.ID);
        if (brazier.ritual instanceof BiomeOnlyRitual ritual) {
            boolean consume = !event.getEntity().hasInfiniteMaterials();
            var stack = event.getItemStack();
            ritual.stage(definition, consume ? stack.split(1) : stack.copyWithCount(1), consume);
            brazier.updateBlock();
        }
    }
}
