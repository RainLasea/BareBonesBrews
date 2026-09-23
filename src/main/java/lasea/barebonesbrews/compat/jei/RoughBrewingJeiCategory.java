package lasea.barebonesbrews.compat.jei;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.compat.RoughBrewingDisplayRecipe;
import lasea.barebonesbrews.compat.client.RoughBrewingRecipeVisuals;
import lasea.barebonesbrews.item.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class RoughBrewingJeiCategory implements IRecipeCategory<RoughBrewingDisplayRecipe> {

    public static final RecipeType<RoughBrewingDisplayRecipe> TYPE = RecipeType.create(
            BareBonesBrews.MODID, "rough_brewing", RoughBrewingDisplayRecipe.class);

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    public RoughBrewingJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemLike(ModItems.ROUGH_POTION.get());
        slot = guiHelper.getSlotDrawable();
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<RoughBrewingDisplayRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.barebonesbrews.rough_brewing");
    }

    @Override
    public int getWidth() {
        return RoughBrewingRecipeVisuals.WIDTH;
    }

    @Override
    public int getHeight() {
        return RoughBrewingRecipeVisuals.HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public ResourceLocation getRegistryName(RoughBrewingDisplayRecipe recipe) {
        return recipe.id();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RoughBrewingDisplayRecipe recipe,
            IFocusGroup focuses) {
        int inputCount = Math.min(RoughBrewingRecipeVisuals.INPUT_SLOTS.length,
                recipe.ingredients().size());
        for (int index = 0; index < inputCount; index++) {
            int[] position = RoughBrewingRecipeVisuals.INPUT_SLOTS[index];
            builder.addSlot(RecipeIngredientRole.INPUT, position[0] + 1, position[1] + 1)
                    .addIngredients(recipe.ingredients().get(index));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, RoughBrewingRecipeVisuals.OUTPUT_X + 1,
                        RoughBrewingRecipeVisuals.OUTPUT_Y + 1)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(RoughBrewingDisplayRecipe recipe, IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics, double mouseX, double mouseY) {
        for (int[] position : RoughBrewingRecipeVisuals.INPUT_SLOTS) {
            slot.draw(graphics, position[0], position[1]);
        }
        slot.draw(graphics, RoughBrewingRecipeVisuals.OUTPUT_X,
                RoughBrewingRecipeVisuals.OUTPUT_Y);
        RoughBrewingRecipeVisuals.draw(graphics, 0, 0, recipe.ingredients(), recipe.output());
        arrow.draw(graphics, RoughBrewingRecipeVisuals.ARROW_X, RoughBrewingRecipeVisuals.ARROW_Y);
        Component duration = Component.translatable("jei.barebonesbrews.duration",
                Math.max(1, (recipe.durationTicks() + 19) / 20));
        int durationX = (getWidth() - Minecraft.getInstance().font.width(duration)) / 2;
        graphics.drawString(Minecraft.getInstance().font, duration, durationX, RoughBrewingRecipeVisuals.DURATION_Y,
                0xFF808080, false);
    }
}
