package lasea.barebonesbrews.brewing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import lasea.barebonesbrews.BareBonesBrews;
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

/** Snapshot of potion output to brewing ingredient, read through narrow mixin accessors. */
public final class BrewingMap {

    private static volatile Map<ResourceLocation, Ingredient> reagents = Map.of();

    private BrewingMap() {}

    public static void bootstrap() {
        capture(PotionBrewing.bootstrap(FeatureFlags.VANILLA_SET, RegistryAccess.EMPTY));
    }

    public static void capture(PotionBrewing brewing) {
        Map<ResourceLocation, Ingredient> captured = new LinkedHashMap<>();
        ResourceLocation water = RoughPotionFactory.idOf(Potions.WATER);
        List<?> mixes = ((PotionBrewingAccessor) brewing).barebonesbrews$getPotionMixes();
        for (Object raw : mixes) {
            PotionMixAccessor mix = (PotionMixAccessor) raw;
            Holder<Potion> from = mix.barebonesbrews$getFrom();
            Holder<Potion> to = mix.barebonesbrews$getTo();
            ResourceLocation fromId = RoughPotionFactory.idOf(from);
            ResourceLocation toId = RoughPotionFactory.idOf(to);
            if (fromId != null && toId != null && !fromId.equals(water)) {
                captured.putIfAbsent(toId, mix.barebonesbrews$getIngredient());
            }
        }
        reagents = Map.copyOf(captured);
        BareBonesBrews.LOGGER.info("Learned brewing ingredients for {} potions", reagents.size());
    }

    public static Map<ResourceLocation, Ingredient> snapshot() {
        return reagents;
    }

    @Nullable
    public static Ingredient reagentOf(ResourceLocation potionId) {
        return reagents.get(potionId);
    }

    public static boolean isReagent(ItemStack stack) {
        return !stack.isEmpty() && reagents.values().stream().anyMatch(ingredient -> ingredient.test(stack));
    }

    public static int size() {
        return reagents.size();
    }
}
