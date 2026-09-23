package lasea.barebonesbrews.compat.rei;

import java.util.ArrayList;
import java.util.List;

import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.compat.client.RoughBrewingRecipeVisuals;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

public final class RoughBrewingReiCategory implements DisplayCategory<RoughBrewingReiDisplay> {

    @Override
    public CategoryIdentifier<? extends RoughBrewingReiDisplay> getCategoryIdentifier() {
        return BareBonesBrewsReiPlugin.ROUGH_BREWING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.barebonesbrews.rough_brewing");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ModItems.ROUGH_POTION.get());
    }

    @Override
    public int getDisplayWidth(RoughBrewingReiDisplay display) {
        return RoughBrewingRecipeVisuals.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return RoughBrewingRecipeVisuals.HEIGHT;
    }

    @Override
    public List<Widget> setupDisplay(RoughBrewingReiDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) ->
                RoughBrewingRecipeVisuals.draw(graphics, bounds.x, bounds.y,
                        display.ingredients(), display.output())));

        for (int index = 0; index < RoughBrewingRecipeVisuals.INPUT_SLOTS.length; index++) {
            int[] position = RoughBrewingRecipeVisuals.INPUT_SLOTS[index];
            var slot = Widgets.createSlot(new Point(bounds.x + position[0], bounds.y + position[1]));
            if (index < display.getInputEntries().size()) {
                slot.entries(display.getInputEntries().get(index)).markInput();
            }
            widgets.add(slot);
        }

        widgets.add(Widgets.createArrow(new Point(bounds.x + RoughBrewingRecipeVisuals.ARROW_X,
                bounds.y + RoughBrewingRecipeVisuals.ARROW_Y)));
        if (!display.getOutputEntries().isEmpty()) {
            widgets.add(Widgets.createSlot(new Point(
                            bounds.x + RoughBrewingRecipeVisuals.OUTPUT_X,
                            bounds.y + RoughBrewingRecipeVisuals.OUTPUT_Y))
                    .entries(display.getOutputEntries().get(0)).markOutput());
        }

        Component duration = Component.translatable("jei.barebonesbrews.duration",
                Math.max(1, (display.durationTicks() + 19) / 20));
        widgets.add(Widgets.createLabel(new Point(bounds.getCenterX(), bounds.y + RoughBrewingRecipeVisuals.DURATION_Y), duration)
                .color(0xFF808080).noShadow());
        return widgets;
    }
}
