package lasea.barebonesbrews.compat.rei;

import java.util.List;
import java.util.Optional;

import lasea.barebonesbrews.compat.RoughBrewingDisplayRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class RoughBrewingReiDisplay extends BasicDisplay {

    private final int durationTicks;
    private final List<Ingredient> ingredients;
    private final ItemStack output;

    public RoughBrewingReiDisplay(RoughBrewingDisplayRecipe recipe) {
        super(EntryIngredients.ofIngredients(recipe.ingredients()),
                List.of(EntryIngredients.of(recipe.output())), Optional.of(recipe.id()));
        durationTicks = recipe.durationTicks();
        ingredients = recipe.ingredients();
        output = recipe.output().copy();
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return BareBonesBrewsReiPlugin.ROUGH_BREWING;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public List<Ingredient> ingredients() {
        return ingredients;
    }

    public ItemStack output() {
        return output;
    }
}
