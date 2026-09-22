package lasea.barebonesbrews.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.item.alchemy.PotionBrewing;

@Mixin(PotionBrewing.class)
public interface PotionBrewingAccessor {

    @Accessor("potionMixes")
    List<?> barebonesbrews$getPotionMixes();
}
