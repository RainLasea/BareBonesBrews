package lasea.barebonesbrews.brewing;

import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.Config;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

/** Brewing stands only process drinkable rough potions; throwable forms use the cauldron. */
public final class RoughPotionBrewingRecipe implements IBrewingRecipe {

    @Override
    public boolean isInput(ItemStack input) {
        return Config.roughPotionsEnabled() && input.is(ModItems.ROUGH_POTION.get());
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return Config.roughPotionsEnabled() && BrewingMap.isReagent(ingredient)
                && !ingredient.is(Items.GUNPOWDER)
                && !ingredient.is(Items.DRAGON_BREATH);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        return isInput(input) && isIngredient(ingredient)
                ? RoughPotionBrewing.transform(input, ingredient) : ItemStack.EMPTY;
    }
}
