package lasea.barebonesbrews.item;

import java.util.Optional;

import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;

final class RoughPotionNames {

    private RoughPotionNames() {}

    static Component name(ItemStack stack, String fallbackKey, String namedKey) {
        Holder<Potion> source = RoughPotionFactory.sourcePotion(stack);
        if (source == null) {
            return Component.translatable(fallbackKey);
        }
        String sourceKey = Potion.getName(Optional.of(source),
                Items.POTION.getDescriptionId() + ".effect.");
        return Component.translatable(namedKey, Component.translatable(sourceKey));
    }
}
