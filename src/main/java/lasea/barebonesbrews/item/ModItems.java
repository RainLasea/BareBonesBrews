package lasea.barebonesbrews.item;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item registration and creative-tab population. */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BareBonesBrews.MODID);

    public static final DeferredHolder<Item, RoughPotionItem> ROUGH_POTION = ITEMS.register("rough_potion",
            () -> new RoughPotionItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, RoughSplashPotionItem> ROUGH_SPLASH_POTION =
            ITEMS.register("rough_splash_potion",
                    () -> new RoughSplashPotionItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, RoughLingeringPotionItem> ROUGH_LINGERING_POTION =
            ITEMS.register("rough_lingering_potion",
                    () -> new RoughLingeringPotionItem(new Item.Properties().stacksTo(1)));

    public static final TagKey<Item> ROUGH_BASE_TAG = Tags.Items.MUSHROOMS;

    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            RoughPotionFactory.allStacks().forEach(event::accept);
        }
    }

    private ModItems() {}
}
