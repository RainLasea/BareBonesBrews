package lasea.barebonesbrews.compat.rei;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.compat.RoughBrewingDisplayRecipes;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.brewing.DefaultBrewingDisplay;
import net.minecraft.world.item.Items;

@REIPluginClient
public final class BareBonesBrewsReiPlugin implements REIClientPlugin {

    public static final CategoryIdentifier<RoughBrewingReiDisplay> ROUGH_BREWING =
            CategoryIdentifier.of(BareBonesBrews.MODID, "rough_brewing");

    @Override
    public void registerCategories(CategoryRegistry registry) {
        if (RoughBrewingDisplayRecipes.usesVanillaCauldron()) {
            registry.add(new RoughBrewingReiCategory());
            registry.addWorkstations(ROUGH_BREWING, EntryIngredients.of(Items.CAULDRON));
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        RoughBrewingDisplayRecipes.create().stream()
                .map(RoughBrewingReiDisplay::new)
                .forEach(registry::add);
        registerBrewingStandDisplays(registry);
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.addEntries(RoughPotionFactory.allStacks().stream()
                .map(EntryStacks::of)
                .toList());
    }

    private static void registerBrewingStandDisplays(DisplayRegistry registry) {
        for (var recipe : lasea.barebonesbrews.compat.RoughBrewingStandRecipes.create()) {
            registry.add(new DefaultBrewingDisplay(EntryIngredients.of(recipe.input()),
                    EntryIngredients.ofIngredient(recipe.reagent()), EntryStacks.of(recipe.output())));
        }
    }
}
