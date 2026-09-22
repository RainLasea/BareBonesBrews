package lasea.barebonesbrews.recipe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.Ingredient;

/** Pure recipe-selection logic, separated from pack I/O. */
public final class RoughRecipeBuilder {

    private RoughRecipeBuilder() {}

    /** Keeps only outputs whose reagent identifies exactly one eligible potion. */
    public static Map<ResourceLocation, Ingredient> selectIngredients(
            Map<ResourceLocation, Ingredient> reagentFor) {
        Map<ResourceLocation, Ingredient> selected = new LinkedHashMap<>();
        for (var source : BuiltInRegistries.POTION.holders().toList()) {
            ResourceLocation sourceId = RoughPotionFactory.idOf(source);
            if (sourceId == null || !RoughPotionFactory.isEligible(source)) {
                continue;
            }
            Ingredient reagent = reagentFor.get(sourceId);
            if (reagent != null && !isShared(reagent, sourceId, reagentFor)) {
                selected.put(sourceId, reagent);
            }
        }
        return Map.copyOf(selected);
    }

    private static boolean isShared(Ingredient reagent, ResourceLocation source,
            Map<ResourceLocation, Ingredient> reagentFor) {
        for (Map.Entry<ResourceLocation, Ingredient> entry : reagentFor.entrySet()) {
            if (entry.getKey().equals(source)) {
                continue;
            }
            Holder<Potion> other = BuiltInRegistries.POTION.getHolder(entry.getKey()).orElse(null);
            if (other != null && RoughPotionFactory.isEligible(other)
                    && equivalent(reagent, entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    /** Ingredient instances are rebuilt independently; compare what they accept, not identity. */
    static boolean equivalent(Ingredient left, Ingredient right) {
        List<ItemStack> leftItems = List.of(left.getItems());
        List<ItemStack> rightItems = List.of(right.getItems());
        return leftItems.size() == rightItems.size()
                && leftItems.stream().allMatch(stack -> rightItems.stream().anyMatch(stack2 -> ItemStack.matches(stack, stack2)))
                && rightItems.stream().allMatch(stack -> leftItems.stream().anyMatch(stack2 -> ItemStack.matches(stack, stack2)));
    }

    public static int durationFor(Holder<Potion> source) {
        int duration = 300 + Math.max(0, source.value().getEffects().size() - 1) * 100;
        return source.value().hasInstantEffects() ? Math.max(200, duration - 100) : duration;
    }
}
