package lasea.barebonesbrews.brewing;

import java.util.List;

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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Stateless rules shared by cauldron interaction and ticking. */
public final class CauldronBrewing {

    public static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "heat_sources"));
    public static final int MAX_INGREDIENTS = 5;

    private CauldronBrewing() {}

    public static boolean isHeated(BlockGetter level, BlockPos pos) {
        BlockState heat = level.getBlockState(pos.below());
        return heat.is(HEAT_SOURCES)
                && (!heat.hasProperty(BlockStateProperties.LIT) || heat.getValue(BlockStateProperties.LIT));
    }

    public static boolean isFullWaterCauldron(BlockState state) {
        return state.is(Blocks.WATER_CAULDRON)
                && state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL;
    }

    public static boolean canStoreIngredient(ItemStack stack) {
        return !stack.isEmpty();
    }

    public static RecipeHolder<CauldronBrewingRecipe> findExactRecipe(ServerLevel level, List<ItemStack> stacks) {
        return level.getRecipeManager().getRecipeFor(ModRecipes.ROUGH_BREWING.get(),
                new CauldronRecipeInput(stacks), level).orElse(null);
    }

    public static boolean isBottle(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.GLASS_BOTTLE);
    }

    public static boolean isWaterBottle(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }

    public static boolean isWaterContainer(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.BUCKET)
                || stack.is(net.minecraft.world.item.Items.WATER_BUCKET)
                || isWaterBottle(stack);
    }
}
