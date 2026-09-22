package lasea.barebonesbrews.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

/** Bridges generated rough recipes into Hexalia without linking against Hexalia at runtime. */
@Pseudo
@Mixin(targets = "net.astralya.hexalia.gameplay.smallcauldron.SmallCauldronContents", remap = false)
public abstract class HexaliaSmallCauldronContentsMixin {

    private static final String RECIPE_PREFIX = "hexalia_rough/";

    @Shadow(remap = false) private String lockedRecipeId;
    @Shadow(remap = false) private ItemStack mixtureResult;

    @Inject(method = "tryStartCooking", at = @At("RETURN"), remap = false)
    private void barebonesbrews$attachRoughPotion(ServerLevel level,
            CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue() || lockedRecipeId == null
                || !mixtureResult.is(ModItems.ROUGH_POTION.get())) {
            return;
        }
        ResourceLocation recipeId = ResourceLocation.tryParse(lockedRecipeId);
        if (recipeId == null || !recipeId.getNamespace().equals("barebonesbrews")
                || !recipeId.getPath().startsWith(RECIPE_PREFIX)) {
            return;
        }
        String encoded = recipeId.getPath().substring(RECIPE_PREFIX.length());
        int separator = encoded.indexOf('/');
        if (separator <= 0 || separator == encoded.length() - 1) {
            return;
        }
        ResourceLocation potionId = ResourceLocation.tryBuild(
                encoded.substring(0, separator), encoded.substring(separator + 1));
        if (potionId == null) {
            return;
        }
        BuiltInRegistries.POTION.getHolder(potionId)
                .map(RoughPotionFactory::create)
                .filter(stack -> !stack.isEmpty())
                .ifPresent(stack -> mixtureResult = stack);
    }

    @Inject(method = "getMixtureColor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void barebonesbrews$roughPotionColor(ItemStack result,
            CallbackInfoReturnable<Integer> callback) {
        if (!result.is(ModItems.ROUGH_POTION.get())) {
            return;
        }
        PotionContents contents = result.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        callback.setReturnValue(contents.getColor());
    }
}
