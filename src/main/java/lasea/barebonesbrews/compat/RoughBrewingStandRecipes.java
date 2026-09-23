package lasea.barebonesbrews.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.brewing.BrewingMap;
import lasea.barebonesbrews.brewing.RoughPotionBrewingRecipe;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** All viewers use the same restricted brewing-stand recipes as gameplay. */
public final class RoughBrewingStandRecipes {
    private RoughBrewingStandRecipes() {}

    public static List<Display> create() {
        List<Display> recipes = new ArrayList<>();
        var brewing = new RoughPotionBrewingRecipe();
        for (BrewingMap.BrewingMix mix : BrewingMap.mixes()) {
            var from = BuiltInRegistries.POTION.getHolder(mix.from()).orElse(null);
            if (from == null || !RoughPotionFactory.isEligible(from)
                    || !RoughPotionFactory.isEligible(mix.to())) {
                continue;
            }
            ItemStack input = RoughPotionFactory.create(from);
            ItemStack output = RoughPotionFactory.create(mix.to());
            var reagents = Arrays.stream(mix.ingredient().getItems())
                    .filter(reagent -> ItemStack.isSameItemSameComponents(
                            brewing.getOutput(input, reagent), output)).toList();
            if (reagents.isEmpty()) {
                continue;
            }
            ResourceLocation target = RoughPotionFactory.idOf(mix.to());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BareBonesBrews.MODID,
                    "brewing/potion/" + mix.from().getNamespace() + "/" + mix.from().getPath()
                            + "_to_" + target.getNamespace() + "/" + target.getPath()
                            + "/" + recipes.size());
            recipes.add(new Display(id, input, Ingredient.of(reagents.stream()), output));
        }
        return List.copyOf(recipes);
    }

    public record Display(ResourceLocation id, ItemStack input, Ingredient reagent, ItemStack output) {}
}
