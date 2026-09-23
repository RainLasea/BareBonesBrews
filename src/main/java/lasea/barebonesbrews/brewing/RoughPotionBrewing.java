package lasea.barebonesbrews.brewing;

import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.Config;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;

/** Post-processes a completed rough potion using the vanilla brewing graph. */
public final class RoughPotionBrewing {

    public static final int POST_PROCESS_DURATION = 200;

    private RoughPotionBrewing() {}

    public static ItemStack transform(ItemStack current, ItemStack reagent) {
        if (!Config.roughPotionsEnabled() || !RoughPotionFactory.isRoughPotion(current) || reagent.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Holder<Potion> source = RoughPotionFactory.sourcePotion(current);
        if (source == null || !RoughPotionFactory.isBrewable(source)) {
            return ItemStack.EMPTY;
        }
        if (reagent.is(Items.GUNPOWDER) && current.is(ModItems.ROUGH_POTION.get())) {
            return RoughPotionFactory.createSplash(source);
        }
        if (reagent.is(Items.DRAGON_BREATH)
                && current.is(ModItems.ROUGH_SPLASH_POTION.get())) {
            return RoughPotionFactory.createLingering(source);
        }
        Holder<Potion> target = BrewingMap.mix(source, reagent);
        return target == null ? ItemStack.EMPTY : RoughPotionFactory.createLike(target, current);
    }
}
