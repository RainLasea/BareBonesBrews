package lasea.barebonesbrews.gametest;

import lasea.barebonesbrews.brewing.CauldronBrewBlockEntity;
import lasea.barebonesbrews.fluid.ModFluids;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import snownee.jade.util.JadeForgeUtils;

import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;

@Mod("barebonesbrews_tests")
@GameTestHolder("barebonesbrews_tests")
@PrefixGameTestTemplate(false)
public final class CauldronGameTests {
    private static final BlockPos POS = new BlockPos(1, 2, 1);

    @GameTest(template = "empty", timeoutTicks = 360)
    public static void fiveIngredientsBrewStrongLingeringPotion(GameTestHelper helper) {
        helper.setBlock(POS.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(POS, Blocks.CAULDRON);
        handler(helper).fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
        for (var ingredient : new net.minecraft.world.item.Item[] {
                Items.BROWN_MUSHROOM, Items.SUGAR, Items.GLOWSTONE_DUST, Items.GUNPOWDER, Items.DRAGON_BREATH}) {
            helper.assertTrue(brew(helper).addIngredient(helper.getLevel(), ingredient.getDefaultInstance()),
                    "Accept ingredient " + ingredient);
        }
        helper.assertTrue(!brew(helper).addIngredient(helper.getLevel(), Items.DIRT.getDefaultInstance()),
                "Reject sixth ingredient");
        helper.runAfterDelay(310, () -> {
            helper.assertTrue(brew(helper).isReady(), "Five-ingredient brew completed");
            var expected = RoughPotionFactory.createLingering(Potions.STRONG_SWIFTNESS);
            for (int i = 0; i < 3; i++) {
                var bottle = brew(helper).takeResult(helper.getLevel());
                helper.assertTrue(bottle != null && ItemStack.isSameItemSameComponents(bottle, expected),
                        "Bottle preserves lingering form and strengthened source potion");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void realBlockReplacementConservesFluid(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.CAULDRON);
        IFluidHandler original = handler(helper);
        helper.assertTrue(original.fill(new FluidStack(Fluids.WATER, 17), EXECUTE) == 17, "Accept 17 mB");
        helper.assertTrue(handler(helper).getFluidInTank(0).getAmount() == 17, "New block entity preserves 17 mB");
        helper.assertTrue(original.drain(17, EXECUTE).isEmpty(), "Removed block entity cannot transfer fluids");
        helper.assertTrue(handler(helper).drain(1000, EXECUTE).getAmount() == 17, "Extract precisely 17 mB");
        helper.assertTrue(helper.getBlockState(POS).is(Blocks.CAULDRON), "Empty again");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pipesBottlesAndJadeReadTheSamePotion(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.CAULDRON);
        handler(helper).fill(ModFluids.fromBottle(RoughPotionFactory.create(Potions.SWIFTNESS), 750), EXECUTE);
        var jade = JadeForgeUtils.fromFluidHandlerStream(handler(helper));
        var displayed = jade.stream.findFirst().orElseThrow().getA();
        helper.assertTrue(displayed.getType() == ModFluids.POTION.get(), "Jade sees potion, not water");
        helper.assertTrue(displayed.getAmount() == 750, "Jade sees actual volume");
        var groups = JadeForgeUtils.fromFluidHandler(handler(helper),
                RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess()));
        helper.assertTrue(!groups.isEmpty(), "Jade can serialize the fluid with its data components");
        handler(helper).drain(137, EXECUTE);
        helper.assertTrue(brew(helper).takeResult(helper.getLevel()) != null, "First bottle");
        helper.assertTrue(brew(helper).takeResult(helper.getLevel()) != null, "Second bottle");
        helper.assertTrue(brew(helper).takeResult(helper.getLevel()) == null, "113 mB cannot become a full bottle");
        helper.assertTrue(handler(helper).drain(1000, EXECUTE).getAmount() == 113, "Pipes can recover the remainder");
        helper.assertTrue(helper.getBlockState(POS).is(Blocks.CAULDRON), "No residual water");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 360)
    public static void fullBrewingCycleCreatesThreeBottles(GameTestHelper helper) {
        helper.setBlock(POS.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(POS, Blocks.CAULDRON);
        helper.assertTrue(handler(helper).fill(new FluidStack(Fluids.WATER, 1000), EXECUTE) == 1000, "Fill one bucket");
        helper.assertTrue(brew(helper).addIngredient(helper.getLevel(), Items.BROWN_MUSHROOM.getDefaultInstance()), "Add base");
        helper.assertTrue(brew(helper).addIngredient(helper.getLevel(), Items.SUGAR.getDefaultInstance()), "Add sugar");
        helper.assertTrue(handler(helper).drain(1000, EXECUTE).isEmpty(), "Brewing locks extraction");
        helper.runAfterDelay(310, () -> {
            helper.assertTrue(brew(helper).isReady(), "Brewing completed on real server ticks");
            helper.assertTrue(handler(helper).getFluidInTank(0).getAmount() == 750, "Three 250 mB doses");
            for (int i = 0; i < 3; i++) {
                helper.assertTrue(brew(helper).takeResult(helper.getLevel()) != null, "Bottle " + (i + 1));
            }
            helper.assertTrue(helper.getBlockState(POS).is(Blocks.CAULDRON), "Third bottle empties the cauldron");
            helper.succeed();
        });
    }

    private static CauldronBrewBlockEntity brew(GameTestHelper helper) {
        return (CauldronBrewBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(POS));
    }

    private static IFluidHandler handler(GameTestHelper helper) {
        IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(POS), Direction.UP);
        helper.assertTrue(handler != null, "Fluid capability is registered");
        return handler;
    }
}
