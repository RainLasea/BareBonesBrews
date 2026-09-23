package lasea.barebonesbrews;

import java.lang.reflect.Method;
import java.util.List;

import lasea.barebonesbrews.brewing.CauldronBrewBlockEntity;
import lasea.barebonesbrews.brewing.CauldronBrewing;
import lasea.barebonesbrews.brewing.CauldronRecipeInput;
import lasea.barebonesbrews.brewing.RoughPotionBrewing;
import lasea.barebonesbrews.brewing.RoughPotionBrewingRecipe;
import lasea.barebonesbrews.compat.RoughBrewingDisplayRecipes;
import lasea.barebonesbrews.compat.RoughBrewingStandRecipes;
import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import lasea.barebonesbrews.recipe.CauldronBrewingRecipe;
import lasea.barebonesbrews.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(EphemeralTestServerProvider.class)
class BrewingRegressionTest {
    private static final BlockPos POS = new BlockPos(0, 64, 0);

    @Test
    void throwablePotionsUseCauldronInsteadOfBrewingStand(MinecraftServer server) {
        var stand = new RoughPotionBrewingRecipe();
        ItemStack drinkable = RoughPotionFactory.create(Potions.SWIFTNESS);
        ItemStack splash = RoughPotionFactory.createSplash(Potions.SWIFTNESS);
        ItemStack lingering = RoughPotionFactory.createLingering(Potions.SWIFTNESS);
        ItemStack glowstone = Items.GLOWSTONE_DUST.getDefaultInstance();
        assertTrue(ItemStack.isSameItemSameComponents(stand.getOutput(drinkable, glowstone),
                RoughPotionFactory.create(Potions.STRONG_SWIFTNESS)));
        assertFalse(stand.isInput(splash));
        assertFalse(stand.isInput(lingering));
        assertFalse(stand.isIngredient(Items.GUNPOWDER.getDefaultInstance()));
        assertFalse(stand.isIngredient(Items.DRAGON_BREATH.getDefaultInstance()));
        assertTrue(stand.getOutput(drinkable, Items.GUNPOWDER.getDefaultInstance()).isEmpty());
        assertTrue(stand.getOutput(splash, Items.DRAGON_BREATH.getDefaultInstance()).isEmpty());
        assertTrue(stand.getOutput(splash, glowstone).isEmpty());
        assertTrue(stand.getOutput(lingering, glowstone).isEmpty());
        assertTrue(ItemStack.isSameItemSameComponents(
                RoughPotionBrewing.transform(drinkable, Items.GUNPOWDER.getDefaultInstance()), splash));
        assertTrue(ItemStack.isSameItemSameComponents(
                RoughPotionBrewing.transform(splash, Items.DRAGON_BREATH.getDefaultInstance()), lingering));
        assertTrue(ItemStack.isSameItemSameComponents(RoughPotionBrewing.transform(splash, glowstone),
                RoughPotionFactory.createSplash(Potions.STRONG_SWIFTNESS)));
        assertTrue(ItemStack.isSameItemSameComponents(RoughPotionBrewing.transform(lingering, glowstone),
                RoughPotionFactory.createLingering(Potions.STRONG_SWIFTNESS)));
    }

    @Test
    void fifthIngredientSurvivesReloadAndSixthIsRejected(MinecraftServer server) {
        ServerLevel level = level(server);
        when(level.getRecipeManager()).thenReturn(server.getRecipeManager());
        var state = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
        var brew = new CauldronBrewBlockEntity(POS, state);
        brew.setLevel(level);
        for (int i = 0; i < 5; i++) {
            assertTrue(brew.addIngredient(level, Items.COBBLESTONE.getDefaultInstance()));
        }
        assertFalse(brew.addIngredient(level, Items.DIRT.getDefaultInstance()));
        var restored = new CauldronBrewBlockEntity(POS, state);
        restored.loadWithComponents(brew.saveWithoutMetadata(server.registryAccess()), server.registryAccess());
        restored.setLevel(level);
        assertEquals(5, restored.floatingItems().size());
        assertFalse(restored.addIngredient(level, Items.DIRT.getDefaultInstance()));
        assertTrue(restored.takeOldestIngredient(level).is(Items.COBBLESTONE));
        assertTrue(restored.addIngredient(level, Items.DIRT.getDefaultInstance()));
        assertTrue(restored.floatingItems().getLast().is(Items.DIRT));
    }

    @Test
    void fiveIngredientLingeringRecipeMatchesViewerAndServer(MinecraftServer server) {
        var input = input(Items.DRAGON_BREATH.getDefaultInstance(), Items.GLOWSTONE_DUST.getDefaultInstance(),
                Items.BROWN_MUSHROOM.getDefaultInstance(), Items.GUNPOWDER.getDefaultInstance(),
                Items.SUGAR.getDefaultInstance());
        var recipe = server.getRecipeManager().getRecipeFor(ModRecipes.ROUGH_BREWING.get(),
                input, server.overworld()).orElseThrow();
        ItemStack expected = RoughPotionFactory.createLingering(Potions.STRONG_SWIFTNESS);
        assertTrue(ItemStack.isSameItemSameComponents(recipe.value().assemble(input, server.registryAccess()), expected));
        var displays = RoughBrewingDisplayRecipes.create();
        assertTrue(displays.stream().anyMatch(display -> display.ingredients().size() == 5
                && ItemStack.isSameItemSameComponents(display.output(), expected)));
        assertTrue(displays.stream().allMatch(display -> display.ingredients().size() <= 5));
        // Every shown ingredient chain must be craftable, including the newly added fifth slot.
        for (var display : displays) {
            var shownInput = new CauldronRecipeInput(display.ingredients().stream()
                    .map(ingredient -> ingredient.getItems()[0]).toList());
            var actual = server.getRecipeManager().getRecipeFor(ModRecipes.ROUGH_BREWING.get(),
                    shownInput, server.overworld()).orElseThrow();
            assertTrue(ItemStack.isSameItemSameComponents(
                    actual.value().getResultItem(server.registryAccess()), display.output()), display.id().toString());
        }
    }

    @Test
    void standViewersOnlyShowAllowedDrinkableRecipes(MinecraftServer server) {
        var recipes = RoughBrewingStandRecipes.create();
        assertFalse(recipes.isEmpty());
        var stand = new RoughPotionBrewingRecipe();
        for (var recipe : recipes) {
            assertTrue(recipe.input().is(ModItems.ROUGH_POTION.get()));
            assertTrue(recipe.output().is(ModItems.ROUGH_POTION.get()));
            for (var reagent : recipe.reagent().getItems()) {
                assertTrue(ItemStack.isSameItemSameComponents(stand.getOutput(recipe.input(), reagent), recipe.output()));
            }
        }
    }

    @Test
    void overlappingIngredientsMatchInEitherOrder(MinecraftServer server) {
        var recipe = CauldronBrewingRecipe.of(List.of(
                Ingredient.of(Items.BROWN_MUSHROOM, Items.RED_MUSHROOM),
                Ingredient.of(Items.BROWN_MUSHROOM)), Items.SUGAR.getDefaultInstance(), 20, 0);
        assertTrue(recipe.matches(input(Items.BROWN_MUSHROOM.getDefaultInstance(),
                Items.RED_MUSHROOM.getDefaultInstance()), null));
        assertTrue(recipe.matches(input(Items.RED_MUSHROOM.getDefaultInstance(),
                Items.BROWN_MUSHROOM.getDefaultInstance()), null));
        assertFalse(recipe.matches(input(Items.RED_MUSHROOM.getDefaultInstance(),
                Items.RED_MUSHROOM.getDefaultInstance()), null));
        assertFalse(recipe.matches(input(Items.BROWN_MUSHROOM.getDefaultInstance()), null));
    }

    @Test
    void extinguishedCampfiresDoNotHeat(MinecraftServer server) {
        BlockGetter level = mock(BlockGetter.class);
        when(level.getBlockState(POS.below())).thenReturn(Blocks.CAMPFIRE.defaultBlockState()
                .setValue(BlockStateProperties.LIT, false));
        assertFalse(CauldronBrewing.isHeated(level, POS));
        when(level.getBlockState(POS.below())).thenReturn(Blocks.CAMPFIRE.defaultBlockState()
                .setValue(BlockStateProperties.LIT, true));
        assertTrue(CauldronBrewing.isHeated(level, POS));
        when(level.getBlockState(POS.below())).thenReturn(Blocks.MAGMA_BLOCK.defaultBlockState());
        assertTrue(CauldronBrewing.isHeated(level, POS));
        when(level.getBlockState(POS.below())).thenReturn(Blocks.STONE.defaultBlockState());
        assertFalse(CauldronBrewing.isHeated(level, POS));
    }

    @Test
    void generatedRecipesLoadWithPotionComponents(MinecraftServer server) {
        var recipes = server.getRecipeManager().getAllRecipesFor(ModRecipes.ROUGH_BREWING.get());
        assertFalse(recipes.isEmpty());
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.value().matches(input(
                Items.BROWN_MUSHROOM.getDefaultInstance(), Items.SUGAR.getDefaultInstance()), null)));
        for (var recipe : recipes) {
            ItemStack result = recipe.value().getResultItem(server.registryAccess());
            assertNotNull(RoughPotionFactory.sourcePotion(result));
            assertNotNull(result.get(DataComponents.POTION_CONTENTS));
        }
    }

    @Test
    void cancellingPostProcessingAfterReloadRestoresBrew(MinecraftServer server) {
        ServerLevel level = level(server);
        ItemStack original = RoughPotionFactory.create(Potions.SWIFTNESS);
        CauldronBrewBlockEntity brew = readyBrew(server, level, original);
        assertTrue(brew.addIngredient(level, Items.GUNPOWDER.getDefaultInstance()));
        CompoundTag saved = brew.saveWithoutMetadata(server.registryAccess());
        CauldronBrewBlockEntity restored = new CauldronBrewBlockEntity(POS, brew.getBlockState());
        restored.loadWithComponents(saved, server.registryAccess());
        restored.setLevel(level);
        assertTrue(restored.takeOldestIngredient(level).is(Items.GUNPOWDER));
        assertTrue(restored.isReady());
        assertTrue(ItemStack.isSameItemSameComponents(original,
                ItemStack.parseOptional(server.registryAccess(),
                        restored.saveWithoutMetadata(server.registryAccess()).getCompound("Result"))));
        assertEquals(750, restored.fluidHandler().getFluidInTank(0).getAmount());
    }

    @Test
    void externalWaterChangeDiscardsBrew(MinecraftServer server) {
        CauldronBrewBlockEntity brew = readyBrew(server, level(server),
                RoughPotionFactory.create(Potions.SWIFTNESS));
        brew.setBlockState(brew.getBlockState().setValue(LayeredCauldronBlock.LEVEL, 2));
        assertFalse(brew.hasBrew());
        assertFalse(brew.isReady());
    }

    @Test
    void bottlingPreservesExactlyTheRemainingServings(MinecraftServer server) {
        ServerLevel level = level(server);
        CauldronBrewBlockEntity brew = readyBrew(server, level,
                RoughPotionFactory.create(Potions.SWIFTNESS));
        when(level.setBlockAndUpdate(any(), any())).thenAnswer(call -> {
            brew.setBlockState(call.getArgument(1));
            return true;
        });
        assertNotNull(brew.takeResult(level));
        assertTrue(brew.isReady());
        assertEquals(2, brew.fillLevel());
        assertNotNull(brew.takeResult(level));
        assertNotNull(brew.takeResult(level));
        assertNull(brew.takeResult(level));
        assertFalse(brew.hasBrew());
    }

    @Test
    void lingeringPotionCreatesCloudWithQuarterDuration(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        ThrownPotion thrown = new ThrownPotion(EntityType.POTION, level);
        ItemStack potion = RoughPotionFactory.createLingering(Potions.SWIFTNESS);
        thrown.setItem(potion);
        Method isLingering = ThrownPotion.class.getDeclaredMethod("isLingering");
        isLingering.setAccessible(true);
        assertEquals(true, isLingering.invoke(thrown));
        PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
        Method cloud = ThrownPotion.class.getDeclaredMethod("makeAreaOfEffectCloud", PotionContents.class);
        cloud.setAccessible(true);
        cloud.invoke(thrown, contents);
        ArgumentCaptor<AreaEffectCloud> captured = ArgumentCaptor.forClass(AreaEffectCloud.class);
        verify(level).addFreshEntity(captured.capture());
        var cloudContents = AreaEffectCloud.class.getDeclaredField("potionContents");
        cloudContents.setAccessible(true);
        assertEquals(contents.customEffects().getFirst().getDuration() / 4,
                ((PotionContents) cloudContents.get(captured.getValue())).customEffects().getFirst().getDuration());
        thrown.setItem(RoughPotionFactory.createSplash(Potions.SWIFTNESS));
        assertEquals(false, isLingering.invoke(thrown));
        thrown.setItem(Items.LINGERING_POTION.getDefaultInstance());
        assertEquals(true, isLingering.invoke(thrown));
    }

    @Test
    void instantEffectDurationIsPreserved(MinecraftServer server) {
        var effects = RoughPotionFactory.create(Potions.HEALING)
                .get(DataComponents.POTION_CONTENTS).customEffects();
        assertEquals(Potions.HEALING.value().getEffects().getFirst().getDuration(),
                effects.getFirst().getDuration());
    }

    @Test
    void dilutionFloorsNeverStrengthenTheSource(MinecraftServer server) throws Exception {
        Method dilute = RoughPotionFactory.class.getDeclaredMethod("dilute", MobEffectInstance.class);
        dilute.setAccessible(true);
        Config.MIN_AMPLIFIER.set(5);
        try {
            MobEffectInstance source = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0);
            MobEffectInstance result = (MobEffectInstance) dilute.invoke(null, source);
            assertEquals(40, result.getDuration());
            assertEquals(0, result.getAmplifier());
            MobEffectInstance infinite = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 0);
            assertEquals(-1, ((MobEffectInstance) dilute.invoke(null, infinite)).getDuration());
        } finally {
            Config.MIN_AMPLIFIER.set(0);
        }
    }

    @Test
    void heatPausesAndSavedProgressResumes(MinecraftServer server) {
        ServerLevel level = level(server);
        CauldronBrewBlockEntity brew = readyBrew(server, level,
                RoughPotionFactory.create(Potions.SWIFTNESS));
        CompoundTag saved = brew.saveWithoutMetadata(server.registryAccess());
        saved.putBoolean("Ready", false);
        saved.putInt("Progress", 10);
        saved.putInt("Duration", 20);
        brew.loadWithComponents(saved, server.registryAccess());
        brew.serverTick(level);
        assertEquals(10, brew.saveWithoutMetadata(server.registryAccess()).getInt("Progress"));
        when(level.getBlockState(POS.below())).thenReturn(Blocks.MAGMA_BLOCK.defaultBlockState());
        brew.serverTick(level);
        saved = brew.saveWithoutMetadata(server.registryAccess());
        assertEquals(11, saved.getInt("Progress"));
        CauldronBrewBlockEntity restored = new CauldronBrewBlockEntity(POS, brew.getBlockState());
        restored.loadWithComponents(saved, server.registryAccess());
        for (int tick = 0; tick < 9; tick++) {
            restored.serverTick(level);
        }
        assertTrue(restored.isReady());
        assertEquals(20, restored.saveWithoutMetadata(server.registryAccess()).getInt("Progress"));
    }

    @Test
    void disabledBrewingCannotTransformExistingPotions(MinecraftServer server) {
        Config.ENABLE_ROUGH_POTIONS.set(false);
        try {
            assertTrue(RoughPotionBrewing.transform(RoughPotionFactory.create(Potions.SWIFTNESS),
                    Items.GUNPOWDER.getDefaultInstance()).isEmpty());
        } finally {
            Config.ENABLE_ROUGH_POTIONS.set(true);
        }
    }

    private static CauldronRecipeInput input(ItemStack... stacks) {
        return new CauldronRecipeInput(List.of(stacks));
    }

    private static ServerLevel level(MinecraftServer server) {
        ServerLevel level = mock(ServerLevel.class);
        when(level.registryAccess()).thenReturn(server.registryAccess());
        when(level.getGameTime()).thenReturn(100L);
        when(level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
        return level;
    }

    private static CauldronBrewBlockEntity readyBrew(MinecraftServer server, ServerLevel level,
            ItemStack result) {
        var state = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
        CauldronBrewBlockEntity brew = new CauldronBrewBlockEntity(POS, state);
        CompoundTag saved = new CompoundTag();
        saved.putString("Recipe", "barebonesbrews:test");
        saved.putBoolean("Ready", true);
        saved.putInt("Servings", 3);
        saved.put("Result", result.save(server.registryAccess()));
        brew.loadWithComponents(saved, server.registryAccess());
        brew.setLevel(level);
        return brew;
    }
}
