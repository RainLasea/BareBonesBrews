package lasea.barebonesbrews.compat;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Recipe-view data shared by JEI, REI and EMI. */
public record RoughBrewingDisplayRecipe(ResourceLocation id, List<Ingredient> ingredients,
        ItemStack output, int durationTicks) {

    public RoughBrewingDisplayRecipe {
        ingredients = List.copyOf(ingredients);
        output = output.copy();
    }
}
