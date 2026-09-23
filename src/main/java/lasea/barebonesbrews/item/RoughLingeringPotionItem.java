package lasea.barebonesbrews.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;

public final class RoughLingeringPotionItem extends LingeringPotionItem {

    public RoughLingeringPotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return RoughPotionNames.name(stack, getDescriptionId(),
                "item.barebonesbrews.rough_lingering_potion.named");
    }
}
