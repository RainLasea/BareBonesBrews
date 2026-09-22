package lasea.barebonesbrews.recipe;

import java.util.List;

import com.mojang.serialization.Codec;
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

/**
 * Brewing performed in a <em>vanilla</em> cauldron.
 *
 * <p>Deliberately modelled on Hexalia's small cauldron recipe: ingredients are unordered and the
 * match requires the <em>exact</em> count to line up, which is what makes "mushroom + the potion's
 * original brewing ingredient" unambiguous.
 *
 * <p>The JSON shape is:
 * <pre>{@code
 * {
 *   "type": "barebonesbrews:rough_brewing",
 *   "ingredients": [ { "tag": "barebonesbrews:rough_base" }, { "item": "minecraft:blaze_powder" } ],
 *   "result": {
 *     "id": "barebonesbrews:rough_potion",
 *     "count": 1,
 *     "components": {
 *       "barebonesbrews:source_potion": "minecraft:strength",
 *       "minecraft:potion_contents": { "custom_effects": [ ... ] }
 *     }
 *   },
 *   "duration": 400,
 *   "experience": 0.1
 * }
 * }</pre>
 */
public final class CauldronBrewingRecipe implements Recipe<RecipeInput> {

    public static final int DEFAULT_DURATION = 400;

    /**
     * The full item stack codec, so a result can carry data components.
     *
     * <p>A rough potion <em>is</em> its {@code minecraft:potion_contents} component, so the result has
     * to be able to express one. The older {@code {"item": ..., "count": ...}} shape that Hexalia and
     * vanilla potions use cannot, which is why this recipe type exists in the first place.
     */
    private static final Codec<ItemStack> RESULT_CODEC = ItemStack.CODEC;

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
        int filled = 0;
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                filled++;
            }
        }
        if (filled != ingredients.size()) {
            return false;
        }

        boolean[] used = new boolean[input.size()];
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int i = 0; i < input.size(); i++) {
                if (!used[i] && ingredient.test(input.getItem(i))) {
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
        // Keeps these out of the recipe book; they are cauldron-only.
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

    /** Static factories used by the runtime recipe generator. */
    public static CauldronBrewingRecipe of(List<Ingredient> ingredients, ItemStack result, int duration, float experience) {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(ingredients);
        return new CauldronBrewingRecipe(list, result, duration, experience);
    }

    public static final class Serializer implements RecipeSerializer<CauldronBrewingRecipe> {

        private static final MapCodec<CauldronBrewingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")
                        .xmap(list -> {
                            NonNullList<Ingredient> out = NonNullList.create();
                            out.addAll(list);
                            return out;
                        }, list -> list)
                        .forGetter(CauldronBrewingRecipe::getIngredients),
                RESULT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Codec.INT.optionalFieldOf("duration", DEFAULT_DURATION).forGetter(CauldronBrewingRecipe::getDuration),
                Codec.FLOAT.optionalFieldOf("experience", 0.0F).forGetter(CauldronBrewingRecipe::getExperience))
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
