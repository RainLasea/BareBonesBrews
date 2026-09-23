package lasea.barebonesbrews.compat.emi;

import java.util.List;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import lasea.barebonesbrews.compat.RoughBrewingStandRecipes;
import net.minecraft.resources.ResourceLocation;

/** One-bottle view, matching the existing JEI and REI brewing displays. */
public final class RoughBrewingStandEmiRecipe extends BasicEmiRecipe {
    public RoughBrewingStandEmiRecipe(RoughBrewingStandRecipes.Display recipe) {
        super(VanillaEmiRecipeCategories.BREWING,
                ResourceLocation.fromNamespaceAndPath(recipe.id().getNamespace(), "/" + recipe.id().getPath()),
                134, 48);
        inputs = List.of(EmiStack.of(recipe.input()), EmiIngredient.of(recipe.reagent()));
        outputs = List.of(EmiStack.of(recipe.output()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 4, 26);
        widgets.addSlot(inputs.get(1), 28, 2);
        widgets.addFillingArrow(57, 26, 20000);
        widgets.addSlot(outputs.getFirst(), 108, 26).recipeContext(this);
    }
}
