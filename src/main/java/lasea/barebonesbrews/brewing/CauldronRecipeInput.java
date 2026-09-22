package lasea.barebonesbrews.brewing;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The loose ingredients sitting in a cauldron, as a {@link RecipeInput}. */
public final class CauldronRecipeInput implements RecipeInput {

    private final List<ItemStack> items;

    public CauldronRecipeInput(List<ItemStack> items) {
        this.items = items;
    }

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < items.size() ? items.get(index) : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return items.size();
    }
}
