package lasea.barebonesbrews.fluid;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.component.ModComponents;
import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModFluids {
    public static final int BOTTLE_VOLUME = 250;
    public static final int BREW_VOLUME = 3 * BOTTLE_VOLUME;
    public static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, BareBonesBrews.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, BareBonesBrews.MODID);

    public static final DeferredHolder<FluidType, FluidType> POTION_TYPE = TYPES.register("rough_potion",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid_type.barebonesbrews.rough_potion")) {
                @Override
                public Component getDescription(FluidStack stack) {
                    ItemStack bottle = toBottle(stack);
                    return bottle.isEmpty() ? super.getDescription(stack) : bottle.getHoverName();
                }
            });
    public static final DeferredHolder<Fluid, PotionFluid> POTION =
            FLUIDS.register("rough_potion", () -> new PotionFluid(POTION_TYPE));
    public static final DeferredHolder<Fluid, PotionFluid> SPLASH =
            FLUIDS.register("rough_splash_potion", () -> new PotionFluid(POTION_TYPE));
    public static final DeferredHolder<Fluid, PotionFluid> LINGERING =
            FLUIDS.register("rough_lingering_potion", () -> new PotionFluid(POTION_TYPE));

    private ModFluids() {}

    public static FluidStack fromBottle(ItemStack bottle, int amount) {
        if (!RoughPotionFactory.isRoughPotion(bottle) || amount <= 0) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = bottle.is(ModItems.ROUGH_SPLASH_POTION.get()) ? SPLASH.get()
                : bottle.is(ModItems.ROUGH_LINGERING_POTION.get()) ? LINGERING.get() : POTION.get();
        FluidStack stack = new FluidStack(fluid, amount);
        stack.applyComponents(bottle.getComponentsPatch());
        return stack;
    }

    public static boolean isPotion(FluidStack stack) {
        return !stack.isEmpty() && (stack.is(POTION.get()) || stack.is(SPLASH.get()) || stack.is(LINGERING.get()));
    }

    public static boolean isValidPotion(FluidStack stack) {
        return isPotion(stack) && stack.get(ModComponents.SOURCE_POTION.get()) != null
                && stack.get(DataComponents.POTION_CONTENTS) != null;
    }

    public static ItemStack toBottle(FluidStack stack) {
        if (!isPotion(stack)) {
            return ItemStack.EMPTY;
        }
        ItemStack bottle = new ItemStack(stack.is(SPLASH.get()) ? ModItems.ROUGH_SPLASH_POTION.get()
                : stack.is(LINGERING.get()) ? ModItems.ROUGH_LINGERING_POTION.get() : ModItems.ROUGH_POTION.get());
        bottle.applyComponents(stack.getComponentsPatch());
        return bottle;
    }
}
