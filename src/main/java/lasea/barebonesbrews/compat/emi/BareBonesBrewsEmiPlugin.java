package lasea.barebonesbrews.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.compat.RoughBrewingDisplayRecipes;
import lasea.barebonesbrews.compat.RoughBrewingStandRecipes;
import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** Discovered by EMI only on the client, keeping the integration optional. */
@EmiEntrypoint
public final class BareBonesBrewsEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory ROUGH_BREWING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(BareBonesBrews.MODID, "rough_brewing"),
            EmiStack.of(ModItems.ROUGH_POTION.get()));

    @Override
    public void register(EmiRegistry registry) {
        Comparison sourcePotion = Comparison.compareData(stack ->
                RoughPotionFactory.sourceId(stack.getItemStack()));
        registry.setDefaultComparison(ModItems.ROUGH_POTION.get(), sourcePotion);
        registry.setDefaultComparison(ModItems.ROUGH_SPLASH_POTION.get(), sourcePotion);
        registry.setDefaultComparison(ModItems.ROUGH_LINGERING_POTION.get(), sourcePotion);
        // Replace the component-less defaults with searchable potion variants.
        registry.removeEmiStacks(stack -> RoughPotionFactory.isRoughPotion(stack.getItemStack())
                && RoughPotionFactory.sourceId(stack.getItemStack()) == null);
        RoughPotionFactory.allStacks().forEach(stack -> registry.addEmiStack(EmiStack.of(stack)));

        if (RoughBrewingDisplayRecipes.usesVanillaCauldron()) {
            registry.addCategory(ROUGH_BREWING);
            registry.addWorkstation(ROUGH_BREWING, EmiStack.of(Items.CAULDRON));
            // JEMI skips categories with native EMI support, avoiding duplicate cauldron displays.
            RoughBrewingDisplayRecipes.create().forEach(recipe ->
                    registry.addRecipe(new RoughBrewingEmiRecipe(recipe)));
        }
        // JEMI does not import vanilla brewing-category recipes, so register these in both setups.
        RoughBrewingStandRecipes.create().forEach(recipe ->
                registry.addRecipe(new RoughBrewingStandEmiRecipe(recipe)));
    }
}
