package lasea.barebonesbrews.recipe;

import net.minecraft.core.Holder;
import net.minecraft.world.item.alchemy.Potion;

/** Brewing duration shared by generated recipes and recipe viewers. */
public final class RoughRecipeBuilder {

    private RoughRecipeBuilder() {}

    public static int durationFor(Holder<Potion> source) {
        int duration = 300 + Math.max(0, source.value().getEffects().size() - 1) * 100;
        return source.value().hasInstantEffects() ? Math.max(200, duration - 100) : duration;
    }
}
