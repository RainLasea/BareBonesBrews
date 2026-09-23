package lasea.barebonesbrews.mixin;

import lasea.barebonesbrews.brewing.CauldronBrewBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla's discrete water levels must not overwrite component-bearing or fractional fluids. */
@Mixin(LayeredCauldronBlock.class)
public abstract class CauldronFluidInteractionMixin {
    @Inject(method = "handlePrecipitation", at = @At("HEAD"), cancellable = true)
    private void barebonesbrews$precipitation(BlockState state, Level level, BlockPos pos,
            Biome.Precipitation precipitation, CallbackInfo ci) {
        if (barebonesbrews$hasCustomFluid(level, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "receiveStalactiteDrip", at = @At("HEAD"), cancellable = true)
    private void barebonesbrews$drip(BlockState state, Level level, BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (barebonesbrews$hasCustomFluid(level, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void barebonesbrews$extinguish(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (barebonesbrews$hasCustomFluid(level, pos)) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean barebonesbrews$hasCustomFluid(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CauldronBrewBlockEntity brew
                && !brew.hasExactVanillaWaterLevel();
    }
}
