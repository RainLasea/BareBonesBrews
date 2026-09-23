package lasea.barebonesbrews.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import lasea.barebonesbrews.compat.RoughBrewingDisplayRecipes;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import lasea.barebonesbrews.recipe.ModRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Run separately with Hexalia installed and this namespace enabled. No hard Hexalia dependency. */
@GameTestHolder("barebonesbrews_hexalia_tests")
@PrefixGameTestTemplate(false)
public final class HexaliaGameTests {
    @GameTest(template = "empty")
    public static void recipesFitFourSlotsAndRetainEffectsAcrossSync(GameTestHelper helper) {
        helper.assertTrue(!RoughBrewingDisplayRecipes.usesVanillaCauldron(), "Hide vanilla cauldron category");
        helper.assertTrue(RoughBrewingDisplayRecipes.create().isEmpty(), "Hide vanilla cauldron displays");
        helper.assertTrue(helper.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.ROUGH_BREWING.get())
                .isEmpty(), "Only generate Hexalia recipes");
        var recipes = helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.id().getNamespace().equals("barebonesbrews")
                        && holder.id().getPath().startsWith("hexalia_rough/")).toList();
        helper.assertTrue(!recipes.isEmpty(), "Generated Hexalia recipes loaded");
        for (var holder : recipes) {
            var recipe = holder.value();
            helper.assertTrue(recipe.getIngredients().size() >= 1 && recipe.getIngredients().size() <= 4,
                    "Recipe fits four slots: " + holder.id());
            helper.assertTrue(recipe.getIngredients().stream().noneMatch(ingredient ->
                    ingredient.test(Items.BROWN_MUSHROOM.getDefaultInstance())
                            || ingredient.test(Items.RED_MUSHROOM.getDefaultInstance())), "No mushroom base");
            var output = recipe.getResultItem(helper.getLevel().registryAccess());
            helper.assertTrue(RoughPotionFactory.sourcePotion(output) != null, "Recipe output has potion data");
            helper.assertTrue(ItemStack.isSameItemSameComponents(output,
                    synced(recipe, helper).getResultItem(helper.getLevel().registryAccess())),
                    "Client recipe synchronization preserves effects");
        }
        var strong = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(
                "barebonesbrews:hexalia_rough/lingering/minecraft/strong_swiftness")).orElseThrow();
        helper.assertTrue(strong.value().getIngredients().size() == 4, "Strong lingering uses four slots");
        helper.assertTrue(ItemStack.isSameItemSameComponents(
                strong.value().getResultItem(helper.getLevel().registryAccess()),
                RoughPotionFactory.createLingering(Potions.STRONG_SWIFTNESS)), "Displayed effects match output");
        helper.succeed();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Recipe<?> synced(Recipe<?> recipe, GameTestHelper helper) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            StreamCodec codec = recipe.getSerializer().streamCodec();
            codec.encode(buffer, recipe);
            return (Recipe<?>) codec.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    @GameTest(template = "empty")
    public static void nativeHexaliaResultFormatStillLoads(GameTestHelper helper) {
        var serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(ResourceLocation.parse("hexalia:small_cauldron"));
        var json = JsonParser.parseString("""
                {"ingredients":[{"item":"minecraft:sugar"}],
                 "result":{"item":"minecraft:apple","count":2},"duration":20}
                """);
        var recipe = serializer.codec().codec().parse(
                RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess()), json).getOrThrow();
        var output = recipe.getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(output.is(Items.APPLE) && output.getCount() == 2, "Legacy result format is preserved");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hexaliaBrewsFourIngredientsWithoutMushroom(GameTestHelper helper) throws Exception {
        Class<?> type = Class.forName("net.astralya.hexalia.gameplay.smallcauldron.SmallCauldronContents");
        Object contents = type.getConstructor().newInstance();
        var fill = type.getDeclaredMethod("fillWater", int.class);
        fill.setAccessible(true);
        fill.invoke(contents, 1000);
        for (var item : new net.minecraft.world.item.Item[] {
                Items.SUGAR, Items.GLOWSTONE_DUST, Items.GUNPOWDER, Items.DRAGON_BREATH}) {
            helper.assertTrue((boolean) type.getMethod("insertOne", ItemStack.class)
                    .invoke(contents, item.getDefaultInstance()), "Accept ingredient " + item);
        }
        for (int stir = 0; stir < 4; stir++) {
            type.getMethod("stir", ServerLevel.class).invoke(contents, helper.getLevel());
        }
        for (int tick = 0; tick < 300; tick++) {
            type.getMethod("tickServer", ServerLevel.class, boolean.class).invoke(contents, helper.getLevel(), true);
        }
        helper.assertTrue((boolean) type.getMethod("hasMixture").invoke(contents), "Brewing completes");
        var result = type.getDeclaredField("mixtureResult");
        result.setAccessible(true);
        helper.assertTrue(ItemStack.isSameItemSameComponents((ItemStack) result.get(contents),
                RoughPotionFactory.createLingering(Potions.STRONG_SWIFTNESS)), "Brew retains form and effects");
        helper.succeed();
    }
}
