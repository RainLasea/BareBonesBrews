package lasea.barebonesbrews.recipe;

import java.util.List;
import java.util.ArrayList;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

import lasea.barebonesbrews.brewing.CauldronBrewing;
import lasea.barebonesbrews.component.ModComponents;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.core.component.DataComponents;

public final class CauldronBrewingRecipe implements Recipe<RecipeInput> {

    public static final int DEFAULT_DURATION = 400;

    private static final Codec<ItemStack> RESULT_CODEC = ItemStack.CODEC.validate(stack ->
            stack.getCount() == 1 && RoughPotionFactory.isRoughPotion(stack)
                    && stack.has(ModComponents.SOURCE_POTION.get()) && stack.has(DataComponents.POTION_CONTENTS)
                    ? DataResult.success(stack)
                    : DataResult.error(() -> "Cauldron result must be one rough potion with source_potion and potion_contents components"));

    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int duration;
    private final float experience;

    public CauldronBrewingRecipe(NonNullList<Ingredient> ingredients, ItemStack result, int duration, float experience) {
        this.ingredients = ingredients;
        this.result = result;
        this.duration = duration;
        this.experience = experience;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public int getDuration() {
        return duration;
    }

    public float getExperience() {
        return experience;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        List<ItemStack> filled = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                filled.add(input.getItem(i));
            }
        }
        return RecipeMatcher.findMatches(filled, ingredients) != null;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ROUGH_BREWING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ROUGH_BREWING.get();
    }

    public static CauldronBrewingRecipe of(List<Ingredient> ingredients, ItemStack result, int duration, float experience) {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(ingredients);
        return new CauldronBrewingRecipe(list, result, duration, experience);
    }

    public static final class Serializer implements RecipeSerializer<CauldronBrewingRecipe> {

        private static final MapCodec<CauldronBrewingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.listOf(1, CauldronBrewing.MAX_INGREDIENTS).fieldOf("ingredients")
                        .xmap(list -> {
                            NonNullList<Ingredient> out = NonNullList.create();
                            out.addAll(list);
                            return out;
                        }, list -> list)
                        .forGetter(CauldronBrewingRecipe::getIngredients),
                RESULT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("duration", DEFAULT_DURATION).forGetter(CauldronBrewingRecipe::getDuration),
                Codec.floatRange(0.0F, Float.MAX_VALUE).optionalFieldOf("experience", 0.0F).forGetter(CauldronBrewingRecipe::getExperience))
                .apply(instance, CauldronBrewingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CauldronBrewingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<CauldronBrewingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CauldronBrewingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static CauldronBrewingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i = 0; i < size; i++) {
                ingredients.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            int duration = buffer.readVarInt();
            float experience = buffer.readFloat();
            return new CauldronBrewingRecipe(ingredients, result, duration, experience);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, CauldronBrewingRecipe recipe) {
            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeVarInt(recipe.duration);
            buffer.writeFloat(recipe.experience);
        }
    }
}
