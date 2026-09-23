package lasea.barebonesbrews;

import java.util.concurrent.atomic.AtomicReference;

import lasea.barebonesbrews.brewing.CauldronBrewBlockEntity;
import lasea.barebonesbrews.fluid.ModFluids;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(EphemeralTestServerProvider.class)
class CauldronFluidTest {
    private static final BlockPos POS = new BlockPos(0, 64, 0);

    @Test
    void pipeSimulationAndInspectionNeverMutateStorage(MinecraftServer server) {
        Fixture f = new Fixture(server);
        FluidStack potion = potion();
        assertEquals(750, f.handler().fill(potion, SIMULATE));
        assertTrue(f.handler().getFluidInTank(0).isEmpty());
        assertEquals(750, f.handler().fill(potion, EXECUTE));
        CompoundTag before = f.brew().saveWithoutMetadata(server.registryAccess());
        assertEquals(137, f.handler().drain(137, SIMULATE).getAmount());
        f.handler().getFluidInTank(0).setAmount(1);
        assertEquals(before, f.brew().saveWithoutMetadata(server.registryAccess()));
        assertTrue(f.handler().drain(-1, EXECUTE).isEmpty());
        assertTrue(f.handler().drain(0, EXECUTE).isEmpty());
    }

    @Test
    void partialExtractionAndBottlingConserveEveryMillibucket(MinecraftServer server) {
        Fixture f = new Fixture(server);
        f.handler().fill(potion(), EXECUTE);
        assertEquals(137, f.handler().drain(137, EXECUTE).getAmount());
        assertNotNull(f.brew().takeResult(f.level));
        assertNotNull(f.brew().takeResult(f.level));
        assertEquals(113, f.handler().getFluidInTank(0).getAmount());
        assertNull(f.brew().takeResult(f.level));
        assertEquals(113, f.handler().drain(1000, EXECUTE).getAmount());
        assertTrue(f.brew().getBlockState().is(Blocks.CAULDRON));
    }

    @Test
    void differentPotionEffectsAndFormsCannotMix(MinecraftServer server) {
        Fixture f = new Fixture(server);
        f.handler().fill(potion().copyWithAmount(250), EXECUTE);
        assertEquals(0, f.handler().fill(ModFluids.fromBottle(RoughPotionFactory.create(Potions.HEALING), 250), EXECUTE));
        assertEquals(0, f.handler().fill(ModFluids.fromBottle(RoughPotionFactory.createSplash(Potions.SWIFTNESS), 250), EXECUTE));
        assertEquals(0, f.handler().fill(new FluidStack(Fluids.WATER, 250), EXECUTE));
        assertTrue(f.handler().drain(ModFluids.fromBottle(RoughPotionFactory.create(Potions.HEALING), 100), EXECUTE).isEmpty());
        assertEquals(500, f.handler().fill(potion(), EXECUTE));
        assertEquals(750, f.handler().getFluidInTank(0).getAmount());
    }

    @Test
    void arbitraryWaterAmountsSurviveVanillaBlockReplacement(MinecraftServer server) {
        Fixture f = new Fixture(server);
        IFluidHandler original = f.handler();
        assertEquals(17, original.fill(new FluidStack(Fluids.WATER, 17), EXECUTE));
        assertEquals(17, f.handler().getFluidInTank(0).getAmount());
        assertNotSame(original, f.handler());
        assertEquals(0, original.fill(new FluidStack(Fluids.WATER, 1000), EXECUTE));
        assertEquals(17, f.handler().drain(1000, EXECUTE).getAmount());
        assertTrue(f.brew().getBlockState().is(Blocks.CAULDRON));
        assertEquals(1000, f.handler().fill(new FluidStack(Fluids.WATER, 2000), EXECUTE));
        assertEquals(3, f.brew().fillLevel());
    }

    @Test
    void ingredientsLockTransfersUntilTheWaterBecomesPotion(MinecraftServer server) {
        Fixture f = new Fixture(server);
        f.handler().fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
        assertTrue(f.brew().addIngredient(f.level, Items.BROWN_MUSHROOM.getDefaultInstance()));
        assertTrue(f.brew().addIngredient(f.level, Items.SUGAR.getDefaultInstance()));
        assertTrue(f.handler().drain(1000, SIMULATE).isEmpty());
        assertTrue(f.handler().drain(1000, EXECUTE).isEmpty());
        assertTrue(f.handler().getFluidInTank(0).is(Fluids.WATER));
        CompoundTag saved = f.brew().saveWithoutMetadata(server.registryAccess());
        saved.putInt("Progress", saved.getInt("Duration") - 1);
        f.brew().loadWithComponents(saved, server.registryAccess());
        f.brew().serverTick(f.level);
        assertTrue(f.brew().isReady());
        assertEquals(750, f.handler().getFluidInTank(0).getAmount());
        assertTrue(ModFluids.isPotion(f.handler().getFluidInTank(0)));
    }

    @Test
    void savedFluidAndBottlesRetainNamesEffectsAndForm(MinecraftServer server) {
        Fixture f = new Fixture(server);
        ItemStack bottle = RoughPotionFactory.createLingering(Potions.LONG_SWIFTNESS);
        bottle.set(DataComponents.CUSTOM_NAME, Component.literal("Saved brew"));
        f.handler().fill(ModFluids.fromBottle(bottle, 613), EXECUTE);
        CompoundTag saved = f.brew().saveWithoutMetadata(server.registryAccess());
        CauldronBrewBlockEntity copy = new CauldronBrewBlockEntity(POS, f.brew().getBlockState());
        copy.loadWithComponents(saved, server.registryAccess());
        copy.setLevel(f.level);
        assertEquals(613, copy.fluidHandler().getFluidInTank(0).getAmount());
        assertTrue(ItemStack.isSameItemSameComponents(bottle,
                ModFluids.toBottle(copy.fluidHandler().getFluidInTank(0))));
    }

    @Test
    void postProcessingLocksFluidAndCancellationKeepsPartialVolume(MinecraftServer server) {
        Fixture f = new Fixture(server);
        f.handler().fill(potion().copyWithAmount(613), EXECUTE);
        assertTrue(f.brew().addIngredient(f.level, Items.GUNPOWDER.getDefaultInstance()));
        assertTrue(f.handler().drain(1, EXECUTE).isEmpty());
        assertEquals(0, f.handler().fill(potion(), EXECUTE));
        assertTrue(f.brew().takeOldestIngredient(f.level).is(Items.GUNPOWDER));
        assertEquals(613, f.handler().getFluidInTank(0).getAmount());
        assertTrue(f.handler().getFluidInTank(0).is(ModFluids.POTION.get()));
    }

    @Test
    void registeredCapabilityAvailableFromEverySide(MinecraftServer server) {
        Fixture f = new Fixture(server);
        for (Direction side : Direction.values()) {
            assertSame(f.handler(), Capabilities.FluidHandler.BLOCK.getCapability(
                    f.level, POS, f.brew().getBlockState(), f.brew(), side));
        }
        assertSame(f.handler(), Capabilities.FluidHandler.BLOCK.getCapability(
                f.level, POS, f.brew().getBlockState(), f.brew(), null));
    }

    @Test
    void potionCannotBePlacedAsAnEffectLosingWorldFluid(MinecraftServer server) {
        Fixture f = new Fixture(server);
        assertFalse(ModFluids.POTION_TYPE.get().canBePlacedInLevel(f.level, POS, potion()));
        assertEquals(0, f.handler().fill(new FluidStack(ModFluids.POTION.get(), 250), EXECUTE));
    }

    private static FluidStack potion() {
        return ModFluids.fromBottle(RoughPotionFactory.create(Potions.SWIFTNESS), 750);
    }

    /** Models the block-entity replacement that LevelChunk performs for empty/water cauldrons. */
    private static final class Fixture {
        private final ServerLevel level = mock(ServerLevel.class);
        private final AtomicReference<CauldronBrewBlockEntity> current = new AtomicReference<>();

        private Fixture(MinecraftServer server) {
            when(level.registryAccess()).thenReturn(server.registryAccess());
            when(level.getRecipeManager()).thenReturn(server.getRecipeManager());
            when(level.getGameTime()).thenReturn(100L);
            when(level.getBlockEntity(POS)).thenAnswer(call -> current.get());
            when(level.getBlockState(POS)).thenAnswer(call -> current.get().getBlockState());
            when(level.getBlockState(POS.below())).thenReturn(Blocks.MAGMA_BLOCK.defaultBlockState());
            current.set(new CauldronBrewBlockEntity(POS, Blocks.CAULDRON.defaultBlockState()));
            current.get().setLevel(level);
            when(level.setBlockAndUpdate(any(), any())).thenAnswer(call -> {
                BlockState state = call.getArgument(1);
                CauldronBrewBlockEntity old = current.get();
                if (state.getBlock() != old.getBlockState().getBlock()) {
                    old.setRemoved();
                    CauldronBrewBlockEntity replacement = new CauldronBrewBlockEntity(POS, state);
                    replacement.setLevel(level);
                    current.set(replacement);
                } else {
                    old.setBlockState(state);
                }
                return true;
            });
        }

        private CauldronBrewBlockEntity brew() { return current.get(); }
        private IFluidHandler handler() { return brew().fluidHandler(); }
    }
}
