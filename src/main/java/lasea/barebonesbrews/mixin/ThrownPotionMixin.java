package lasea.barebonesbrews.mixin;

import lasea.barebonesbrews.item.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps custom lingering bottles on vanilla's cloud path, including its duration reduction. */
@Mixin(ThrownPotion.class)
public abstract class ThrownPotionMixin {

    @Inject(method = "isLingering", at = @At("HEAD"), cancellable = true)
    private void barebonesbrews$isLingering(CallbackInfoReturnable<Boolean> callback) {
        if (((ThrownPotion) (Object) this).getItem().is(ModItems.ROUGH_LINGERING_POTION.get())) {
            callback.setReturnValue(true);
        }
    }

    @ModifyVariable(method = "makeAreaOfEffectCloud", at = @At("HEAD"), argsOnly = true)
    private PotionContents barebonesbrews$cloudEffects(PotionContents contents) {
        if (!((ThrownPotion) (Object) this).getItem().is(ModItems.ROUGH_LINGERING_POTION.get())) {
            return contents;
        }
        // Vanilla quarters registered potion effects, but leaves custom effects untouched.
        // Rough effects are custom, so scale them here once when the cloud is created.
        return new PotionContents(contents.potion(), contents.customColor(),
                contents.customEffects().stream().map(effect -> new MobEffectInstance(
                        effect.getEffect(), effect.mapDuration(ticks -> ticks / 4),
                        effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()))
                        .toList());
    }
}
