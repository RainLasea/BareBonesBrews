package lasea.barebonesbrews.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import lasea.barebonesbrews.compat.HexaliaCauldronView;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.fml.ModList;

/** Bridges generated rough recipes into Hexalia without linking against Hexalia at runtime. */
@Pseudo
@Mixin(targets = "net.astralya.hexalia.gameplay.smallcauldron.SmallCauldronContents", remap = false)
public abstract class HexaliaSmallCauldronContentsMixin implements HexaliaCauldronView {

    private static final String JADE_INGREDIENTS_TAG = "BareBonesBrewsIngredients";

    @Shadow(remap = false) @Final private NonNullList<ItemStack> ingredients;
    @Shadow(remap = false) public abstract boolean isCooking();
    @Shadow(remap = false) public abstract boolean hasMixture();
    @Shadow(remap = false) public abstract boolean isSpoiled();

    @Unique
    private final List<ItemStack> barebonesbrews$cookingIngredients = new ArrayList<>();

    @Inject(method = "tryStartCooking", at = @At("HEAD"), remap = false)
    private void barebonesbrews$rememberCookingIngredients(ServerLevel level,
            CallbackInfoReturnable<Boolean> callback) {
        barebonesbrews$cookingIngredients.clear();
        if (!ModList.get().isLoaded("jade")) {
            return;
        }
        ingredients.stream().filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy).forEach(barebonesbrews$cookingIngredients::add);
    }

    @Inject(method = "tryStartCooking", at = @At("RETURN"), remap = false)
    private void barebonesbrews$discardFailedIngredientSnapshot(ServerLevel level,
            CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue()) {
            barebonesbrews$cookingIngredients.clear();
        }
    }

    @Inject(method = "finalizeCook", at = @At("RETURN"), remap = false)
    private void barebonesbrews$finishIngredientSnapshot(ServerLevel level,
            CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) {
            barebonesbrews$cookingIngredients.clear();
        }
    }

    @Inject(method = "save", at = @At("RETURN"), remap = false)
    private void barebonesbrews$saveIngredientSnapshot(CompoundTag tag,
            HolderLookup.Provider registries, CallbackInfo callback) {
        if (barebonesbrews$cookingIngredients.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        barebonesbrews$cookingIngredients.forEach(stack -> list.add(stack.save(registries)));
        tag.put(JADE_INGREDIENTS_TAG, list);
    }

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private void barebonesbrews$loadIngredientSnapshot(CompoundTag tag,
            HolderLookup.Provider registries, CallbackInfo callback) {
        barebonesbrews$cookingIngredients.clear();
        if (!isCooking()) {
            return;
        }
        ListTag list = tag.getList(JADE_INGREDIENTS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(index));
            if (!stack.isEmpty()) {
                barebonesbrews$cookingIngredients.add(stack);
            }
        }
    }

    @Inject(method = "getMixtureColor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void barebonesbrews$roughPotionColor(ItemStack result,
            CallbackInfoReturnable<Integer> callback) {
        if (!RoughPotionFactory.isRoughPotion(result)) {
            return;
        }
        PotionContents contents = result.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        callback.setReturnValue(contents.getColor());
    }

    @Override
    public List<ItemStack> barebonesbrews$displayIngredients() {
        List<ItemStack> current = ingredients.stream()
                .filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
        if (!current.isEmpty()) {
            return current;
        }
        return isCooking() ? List.copyOf(barebonesbrews$cookingIngredients) : List.of();
    }

    @Override
    public boolean barebonesbrews$hasActiveContents() {
        return ingredients.stream().anyMatch(stack -> !stack.isEmpty())
                || !barebonesbrews$cookingIngredients.isEmpty()
                || isCooking() || hasMixture() || isSpoiled();
    }

}
