package lasea.barebonesbrews.compat;

import java.util.List;

import lasea.barebonesbrews.Config;
import lasea.barebonesbrews.recipe.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.fml.ModList;

public final class RoughBrewingDisplayRecipes {
    private RoughBrewingDisplayRecipes() {}

    public static List<RoughBrewingDisplayRecipe> create(RecipeManager manager, HolderLookup.Provider registries) {
        if (!usesVanillaCauldron() || !Config.roughPotionsEnabled() || !Config.cauldronRecipesEnabled()) {
            return List.of();
        }
        return manager.getAllRecipesFor(ModRecipes.ROUGH_BREWING.get()).stream()
                .map(holder -> new RoughBrewingDisplayRecipe(holder.id(), holder.value().getIngredients(),
                        holder.value().getResultItem(registries), holder.value().getDuration()))
                .toList();
    }

    public static boolean usesVanillaCauldron() {
        return !ModList.get().isLoaded("hexalia");
    }
}
