package lasea.barebonesbrews.brewing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.jetbrains.annotations.Nullable;

import lasea.barebonesbrews.mixin.PotionBrewingAccessor;
import lasea.barebonesbrews.mixin.PotionMixAccessor;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;

/** Snapshot of potion transitions, read through narrow mixin accessors. */
public final class BrewingMap {

    private static volatile List<PotionTransition> transitions = List.of();

    private BrewingMap() {}

    public static void bootstrap() {
        capture(PotionBrewing.bootstrap(FeatureFlags.VANILLA_SET, RegistryAccess.EMPTY));
    }

    public static void capture(PotionBrewing brewing) {
        List<PotionTransition> capturedTransitions = new ArrayList<>();
        List<?> mixes = ((PotionBrewingAccessor) brewing).barebonesbrews$getPotionMixes();
        for (Object raw : mixes) {
            PotionMixAccessor mix = (PotionMixAccessor) raw;
            Holder<Potion> from = mix.barebonesbrews$getFrom();
            Holder<Potion> to = mix.barebonesbrews$getTo();
            ResourceLocation fromId = RoughPotionFactory.idOf(from);
            ResourceLocation toId = RoughPotionFactory.idOf(to);
            if (fromId != null && toId != null) {
                capturedTransitions.add(new PotionTransition(fromId, to, mix.barebonesbrews$getIngredient()));
            }
        }
        transitions = List.copyOf(capturedTransitions);
    }

    public static boolean isReagent(ItemStack stack) {
        return !stack.isEmpty() && transitions.stream()
                .anyMatch(transition -> transition.ingredient().test(stack));
    }

    /**
     * Returns the shortest vanilla-style ingredient chain from a rough-potion base.
     * Mushrooms replace the water-to-awkward step, so water and awkward are both free starting
     * points. This retains direct water recipes such as weakness without requiring nether wart.
     */
    public static List<BrewingPath> pathsFromRoughBase() {
        if (transitions.isEmpty()) {
            bootstrap();
        }
        ResourceLocation water = RoughPotionFactory.idOf(Potions.WATER);
        ResourceLocation awkward = RoughPotionFactory.idOf(Potions.AWKWARD);
        if (water == null || awkward == null) {
            return List.of();
        }

        Map<ResourceLocation, BrewingPath> discovered = new LinkedHashMap<>();
        Queue<ResourceLocation> pending = new ArrayDeque<>();
        discovered.put(water, new BrewingPath(Potions.WATER, List.of()));
        discovered.put(awkward, new BrewingPath(Potions.AWKWARD, List.of()));
        pending.add(water);
        pending.add(awkward);

        while (!pending.isEmpty()) {
            ResourceLocation source = pending.remove();
            BrewingPath sourcePath = discovered.get(source);
            for (PotionTransition transition : transitions) {
                if (!transition.from().equals(source)) {
                    continue;
                }
                ResourceLocation target = RoughPotionFactory.idOf(transition.to());
                if (target == null || discovered.containsKey(target)) {
                    continue;
                }
                List<Ingredient> ingredients = new ArrayList<>(sourcePath.ingredients());
                ingredients.add(transition.ingredient());
                discovered.put(target, new BrewingPath(transition.to(), ingredients));
                pending.add(target);
            }
        }

        return discovered.values().stream()
                .filter(path -> RoughPotionFactory.isEligible(path.result()))
                .toList();
    }

    @Nullable
    public static Holder<Potion> mix(Holder<Potion> from, ItemStack reagent) {
        ResourceLocation source = RoughPotionFactory.idOf(from);
        if (source == null || reagent.isEmpty()) {
            return null;
        }
        return transitions.stream()
                .filter(transition -> transition.from().equals(source)
                        && transition.ingredient().test(reagent))
                .map(PotionTransition::to)
                .filter(RoughPotionFactory::isBrewable)
                .findFirst().orElse(null);
    }

    public static List<BrewingMix> mixes() {
        if (transitions.isEmpty()) {
            bootstrap();
        }
        return transitions.stream()
                .map(transition -> new BrewingMix(transition.from(), transition.to(),
                        transition.ingredient()))
                .toList();
    }

    public record BrewingPath(Holder<Potion> result, List<Ingredient> ingredients) {
        public BrewingPath {
            ingredients = List.copyOf(ingredients);
        }
    }

    public record BrewingMix(ResourceLocation from, Holder<Potion> to, Ingredient ingredient) {}

    private record PotionTransition(ResourceLocation from, Holder<Potion> to, Ingredient ingredient) {}
}
