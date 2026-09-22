package lasea.barebonesbrews.brewing;

import lasea.barebonesbrews.BareBonesBrews;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

/** Owns only interactions actually consumed by rough brewing. */
@EventBusSubscriber(modid = BareBonesBrews.MODID)
public final class CauldronEvents {

    private CauldronEvents() {}

    @SubscribeEvent
    static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK || event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel level)
                || event.getPlayer() == null) {
            return;
        }
        var state = level.getBlockState(event.getPos());
        if (!state.is(Blocks.WATER_CAULDRON)
                || !(level.getBlockEntity(event.getPos()) instanceof CauldronBrewBlockEntity brew)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack held = player.getItemInHand(event.getHand());
        if (CauldronBrewing.isBottle(held)) {
            ItemStack result = brew.takeResult(level);
            if (result != null) {
                ItemStack replacement = ItemUtils.createFilledResult(held, player, result);
                player.setItemInHand(event.getHand(), replacement);
                event.cancelWithResult(ItemInteractionResult.SUCCESS);
            }
            return;
        }

        if (!CauldronBrewing.isWaterBottle(held) && brew.addIngredient(level, held)) {
            held.consume(1, player);
            event.cancelWithResult(ItemInteractionResult.SUCCESS);
        }
    }
}
