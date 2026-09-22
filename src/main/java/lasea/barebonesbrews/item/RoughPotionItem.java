package lasea.barebonesbrews.item;

import java.util.Optional;

import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;

/** One potion item whose data components identify and describe every rough variant. */
public final class RoughPotionItem extends PotionItem {

    public RoughPotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Holder<Potion> source = RoughPotionFactory.sourcePotion(stack);
        if (source == null) {
            return Component.translatable(getDescriptionId());
        }
        String sourceKey = Potion.getName(Optional.of(source), Items.POTION.getDescriptionId() + ".effect.");
        return Component.translatable("item.barebonesbrews.rough_potion.named",
                Component.translatable(sourceKey));
    }
}
