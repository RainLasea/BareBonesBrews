package lasea.barebonesbrews.brewing;

import lasea.barebonesbrews.BareBonesBrews;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

/** Owns only interactions actually consumed by rough brewing. */
@EventBusSubscriber(modid = BareBonesBrews.MODID)
public final class CauldronEvents {

    private CauldronEvents() {}

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.isCanceled() || player.isSpectator() || !player.isShiftKeyDown()
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.WATER_CAULDRON)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof CauldronBrewBlockEntity brew)
                || !brew.canTakeIngredient()) {
            return;
        }

        // Sneaking with an item skips the BLOCK use phase. Handle withdrawal before that
        // decision, and consume it on both sides so the client does not try the other hand.
        if (event.getLevel() instanceof ServerLevel level) {
            ItemStack returned = brew.takeOldestIngredient(level);
            ItemStack held = player.getItemInHand(event.getHand());
            if (held.isEmpty()) {
                player.setItemInHand(event.getHand(), returned);
            } else {
                if (ItemStack.isSameItemSameComponents(held, returned)) {
                    int moved = Math.min(returned.getCount(),
                            held.getMaxStackSize() - held.getCount());
                    if (moved > 0) {
                        held.grow(moved);
                        returned.shrink(moved);
                    }
                }
                if (!returned.isEmpty() && !player.getInventory().add(returned)) {
                    player.drop(returned, false);
                }
            }
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK || event.isCanceled()
                || event.getPlayer() == null) {
            return;
        }
        var level = event.getLevel();
        var state = level.getBlockState(event.getPos());
        if (!state.is(Blocks.WATER_CAULDRON)
                || !(level.getBlockEntity(event.getPos()) instanceof CauldronBrewBlockEntity brew)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack held = player.getItemInHand(event.getHand());
        if (player.isShiftKeyDown() || player.isSpectator()) {
            return;
        }
        if (CauldronBrewing.isBottle(held)) {
            if (brew.canBottlePotion()) {
                if (level instanceof ServerLevel server) {
                    ItemStack result = brew.takeResult(server);
                    if (result == null) {
                        event.cancelWithResult(ItemInteractionResult.FAIL);
                        return;
                    }
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(held.getItem()));
                    player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(held, player, result));
                    level.gameEvent(player, GameEvent.FLUID_PICKUP, event.getPos());
                }
                event.cancelWithResult(ItemInteractionResult.sidedSuccess(level.isClientSide));
            } else if (brew.containsPotion() || !brew.hasExactVanillaWaterLevel()) {
                // Never let the vanilla water interaction bottle an unfinished or sub-bottle potion.
                event.cancelWithResult(ItemInteractionResult.FAIL);
            }
            return;
        }

        if (CauldronInteraction.WATER.map().containsKey(held.getItem())) {
            if (brew.containsPotion() || (!brew.hasExactVanillaWaterLevel() && !held.is(Items.WATER_BUCKET))) {
                event.cancelWithResult(ItemInteractionResult.FAIL);
            }
            return;
        }

        if (level instanceof ServerLevel server && !CauldronBrewing.isWaterContainer(held)
                && brew.addIngredient(server, held)) {
            held.consume(1, player);
            event.cancelWithResult(ItemInteractionResult.SUCCESS);
        }
    }
}
