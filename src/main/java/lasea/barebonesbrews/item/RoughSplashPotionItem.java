package lasea.barebonesbrews.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SplashPotionItem;

public final class RoughSplashPotionItem extends SplashPotionItem {

    public RoughSplashPotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return RoughPotionNames.name(stack, getDescriptionId(),
                "item.barebonesbrews.rough_splash_potion.named");
    }
}
