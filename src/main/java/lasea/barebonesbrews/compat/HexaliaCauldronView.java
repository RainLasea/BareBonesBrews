package lasea.barebonesbrews.compat;

import java.util.List;

import net.minecraft.world.item.ItemStack;

public interface HexaliaCauldronView {

    List<ItemStack> barebonesbrews$displayIngredients();

    boolean barebonesbrews$hasActiveContents();
}
