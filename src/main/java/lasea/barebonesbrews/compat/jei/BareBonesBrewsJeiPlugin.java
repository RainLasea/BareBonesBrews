package lasea.barebonesbrews.compat.jei;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.compat.RoughBrewingDisplayRecipes;
import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** JEI entry point; JEI discovers this class only when its API is present. */
@JeiPlugin
public final class BareBonesBrewsJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            BareBonesBrews.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> interpreter = new ISubtypeInterpreter<>() {
            @Nullable
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return RoughPotionFactory.sourceId(stack);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                ResourceLocation id = RoughPotionFactory.sourceId(stack);
                return id == null ? "" : id.toString();
            }
        };
        registration.registerSubtypeInterpreter(ModItems.ROUGH_POTION.get(), interpreter);
        registration.registerSubtypeInterpreter(ModItems.ROUGH_SPLASH_POTION.get(), interpreter);
        registration.registerSubtypeInterpreter(ModItems.ROUGH_LINGERING_POTION.get(), interpreter);
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(RoughPotionFactory.allStacks());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (RoughBrewingDisplayRecipes.usesVanillaCauldron()) {
            registration.addRecipeCategories(new RoughBrewingJeiCategory(
                    registration.getJeiHelpers().getGuiHelper()));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (RoughBrewingDisplayRecipes.usesVanillaCauldron()) {
            registration.addRecipes(RoughBrewingJeiCategory.TYPE, RoughBrewingDisplayRecipes.create());
        }
        registration.addRecipes(RecipeTypes.BREWING, createBrewingStandRecipes(registration));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (RoughBrewingDisplayRecipes.usesVanillaCauldron()) {
            registration.addRecipeCatalyst(Items.CAULDRON, RoughBrewingJeiCategory.TYPE);
        }
    }

    private static List<IJeiBrewingRecipe> createBrewingStandRecipes(
            IRecipeRegistration registration) {
        var factory = registration.getVanillaRecipeFactory();
        return lasea.barebonesbrews.compat.RoughBrewingStandRecipes.create().stream()
                .map(recipe -> factory.createBrewingRecipe(Arrays.asList(recipe.reagent().getItems()),
                        recipe.input(), recipe.output(), recipe.id()))
                .toList();
    }
}
