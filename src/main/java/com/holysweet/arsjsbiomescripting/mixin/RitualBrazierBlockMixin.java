package com.holysweet.arsjsbiomescripting.mixin;

import com.hollingsworth.arsnouveau.common.block.RitualBrazierBlock;
import com.hollingsworth.arsnouveau.common.block.tile.RitualBrazierTile;
import com.holysweet.arsjsbiomescripting.ritual.ConfiguredBiomeRitual;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RitualBrazierBlock.class, remap = false)
@SuppressWarnings("unused") // Loaded from arsjsbiomescripting.mixins.json.
abstract class RitualBrazierBlockMixin {
    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void arsjsbiomescripting$returnStagedInput(
            Level level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> callback
    ) {
        if (level.getBlockEntity(pos) instanceof RitualBrazierTile brazier
                && brazier.ritual instanceof ConfiguredBiomeRitual ritual && !ritual.isRunning() && !ritual.isDone()) {
            // Ars drops a registered tablet for staged rituals. Drop this ritual's actual input instead.
            var input = ritual.removeStagedInput();
            brazier.ritual = null;
            if (!level.isClientSide()) {
                if (!input.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), input);
                }
                for (var augment : ritual.getConsumedItems()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), augment);
                }
            }
        }
    }
}
