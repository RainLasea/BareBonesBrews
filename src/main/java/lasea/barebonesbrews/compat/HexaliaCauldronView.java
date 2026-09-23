package lasea.barebonesbrews.compat;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/** Read-only bridge implemented by the optional Hexalia contents mixin. */
public interface HexaliaCauldronView {

    List<ItemStack> barebonesbrews$displayIngredients();

    boolean barebonesbrews$hasActiveContents();
}
