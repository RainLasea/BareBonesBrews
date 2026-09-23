package lasea.barebonesbrews.compat;

import java.util.ArrayList;
import java.util.List;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.Config;
import lasea.barebonesbrews.brewing.BrewingMap;
import lasea.barebonesbrews.brewing.CauldronBrewing;
import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import lasea.barebonesbrews.recipe.RoughRecipeBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.ModList;

public final class RoughBrewingDisplayRecipes {

    private RoughBrewingDisplayRecipes() {
    }

    public static List<RoughBrewingDisplayRecipe> create() {
        if (!usesVanillaCauldron() || !Config.roughPotionsEnabled() || !Config.cauldronRecipesEnabled()) {
            return List.of();
        }

        Ingredient mushroom = Ingredient.of(ModItems.ROUGH_BASE_TAG);
        Ingredient gunpowder = Ingredient.of(Items.GUNPOWDER);
        List<RoughBrewingDisplayRecipe> recipes = new ArrayList<>();

        for (BrewingMap.BrewingPath path : BrewingMap.pathsFromRoughBase()) {
            List<Ingredient> normal = new ArrayList<>();
            normal.add(mushroom);
            normal.addAll(path.ingredients());
            if (normal.size() <= CauldronBrewing.MAX_INGREDIENTS) {
                recipes.add(new RoughBrewingDisplayRecipe(id("potion", path.result()), normal,
                        RoughPotionFactory.create(path.result()),
                        RoughRecipeBuilder.durationFor(path.result())));
            }

            List<Ingredient> splash = new ArrayList<>(normal);
            splash.add(gunpowder);
            if (splash.size() <= CauldronBrewing.MAX_INGREDIENTS) {
                recipes.add(new RoughBrewingDisplayRecipe(id("splash", path.result()), splash,
                        RoughPotionFactory.createSplash(path.result()),
                        RoughRecipeBuilder.durationFor(path.result())));
            }

            List<Ingredient> lingering = new ArrayList<>(splash);
            lingering.add(Ingredient.of(Items.DRAGON_BREATH));
            if (lingering.size() <= CauldronBrewing.MAX_INGREDIENTS) {
                recipes.add(new RoughBrewingDisplayRecipe(id("lingering", path.result()), lingering,
                        RoughPotionFactory.createLingering(path.result()),
                        RoughRecipeBuilder.durationFor(path.result())));
            }
        }
        return List.copyOf(recipes);
    }

    public static boolean usesVanillaCauldron() {
        return !ModList.get().isLoaded("hexalia");
    }

    private static ResourceLocation id(String form, Holder<Potion> potion) {
        ResourceLocation potionId = RoughPotionFactory.idOf(potion);
        String namespace = potionId == null ? "unknown" : potionId.getNamespace();
        String path = potionId == null ? "unknown" : potionId.getPath();
        return ResourceLocation.fromNamespaceAndPath(BareBonesBrews.MODID,
                "rough_brewing/" + form + "/" + namespace + "/" + path);
    }
}
