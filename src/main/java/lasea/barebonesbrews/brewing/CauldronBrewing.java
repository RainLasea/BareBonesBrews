package lasea.barebonesbrews.brewing;

import java.util.List;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.recipe.CauldronBrewingRecipe;
import lasea.barebonesbrews.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Stateless rules shared by cauldron interaction and ticking. */
public final class CauldronBrewing {

    public static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(BareBonesBrews.MODID, "cauldron_heat_sources"));
    public static final int MAX_INGREDIENTS = 4;

    private CauldronBrewing() {}

    public static boolean isHeated(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(HEAT_SOURCES);
    }

    public static boolean isFullWaterCauldron(BlockState state) {
        return state.is(Blocks.WATER_CAULDRON)
                && state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL;
    }

    public static boolean isPlausibleIngredient(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ModItems.ROUGH_BASE_TAG) || BrewingMap.isReagent(stack));
    }

    public static RecipeHolder<CauldronBrewingRecipe> findExactRecipe(ServerLevel level, List<ItemStack> stacks) {
        return level.getRecipeManager().getRecipeFor(ModRecipes.ROUGH_BREWING.get(),
                new CauldronRecipeInput(stacks), level).orElse(null);
    }

    public static boolean couldStillMatch(ServerLevel level, List<ItemStack> stacks) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.ROUGH_BREWING.get()).stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> isSubset(recipe.getIngredients(), stacks));
    }

    private static boolean isSubset(List<Ingredient> ingredients, List<ItemStack> stacks) {
        if (stacks.size() > ingredients.size()) {
            return false;
        }
        boolean[] used = new boolean[ingredients.size()];
        for (ItemStack stack : stacks) {
            boolean found = false;
            for (int i = 0; i < ingredients.size(); i++) {
                if (!used[i] && ingredients.get(i).test(stack)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static boolean isBottle(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.GLASS_BOTTLE);
    }

    public static boolean isWaterBottle(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }
}
