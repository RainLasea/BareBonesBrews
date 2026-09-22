package lasea.barebonesbrews.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.Ingredient;

@Mixin(targets = "net.minecraft.world.item.alchemy.PotionBrewing$Mix")
public interface PotionMixAccessor {

    @Accessor("from")
    Holder<Potion> barebonesbrews$getFrom();

    @Accessor("ingredient")
    Ingredient barebonesbrews$getIngredient();

    @Accessor("to")
    Holder<Potion> barebonesbrews$getTo();
}
