package lasea.barebonesbrews.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;

public final class RoughPotionItem extends PotionItem {

    public RoughPotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return RoughPotionNames.name(stack, getDescriptionId(),
                "item.barebonesbrews.rough_potion.named");
    }
}
