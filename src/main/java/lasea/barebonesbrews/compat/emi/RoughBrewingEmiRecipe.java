package lasea.barebonesbrews.compat.emi;

import java.util.List;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import lasea.barebonesbrews.compat.RoughBrewingDisplayRecipe;
import lasea.barebonesbrews.compat.client.RoughBrewingRecipeVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class RoughBrewingEmiRecipe extends BasicEmiRecipe {
    private final RoughBrewingDisplayRecipe recipe;

    public RoughBrewingEmiRecipe(RoughBrewingDisplayRecipe recipe) {
        super(BareBonesBrewsEmiPlugin.ROUGH_BREWING,
                ResourceLocation.fromNamespaceAndPath(recipe.id().getNamespace(), "/" + recipe.id().getPath()),
                RoughBrewingRecipeVisuals.WIDTH, RoughBrewingRecipeVisuals.HEIGHT);
        this.recipe = recipe;
        inputs = recipe.ingredients().stream().map(EmiIngredient::of).toList();
        outputs = List.of(EmiStack.of(recipe.output()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, width, height, (graphics, mouseX, mouseY, delta) ->
                RoughBrewingRecipeVisuals.draw(graphics, 0, 0, recipe.ingredients(), recipe.output()));
        for (int index = 0; index < RoughBrewingRecipeVisuals.INPUT_SLOTS.length; index++) {
            int[] position = RoughBrewingRecipeVisuals.INPUT_SLOTS[index];
            widgets.addSlot(index < inputs.size() ? inputs.get(index) : EmiStack.EMPTY,
                    position[0], position[1]);
        }
        widgets.addSlot(outputs.getFirst(), RoughBrewingRecipeVisuals.OUTPUT_X,
                RoughBrewingRecipeVisuals.OUTPUT_Y).recipeContext(this);
        widgets.addFillingArrow(RoughBrewingRecipeVisuals.ARROW_X,
                RoughBrewingRecipeVisuals.ARROW_Y, recipe.durationTicks() * 50);
        Component duration = Component.translatable("jei.barebonesbrews.duration",
                Math.max(1, (recipe.durationTicks() + 19) / 20));
        widgets.addText(duration, (width - Minecraft.getInstance().font.width(duration)) / 2,
                RoughBrewingRecipeVisuals.DURATION_Y, 0xFF808080, false);
    }
}
