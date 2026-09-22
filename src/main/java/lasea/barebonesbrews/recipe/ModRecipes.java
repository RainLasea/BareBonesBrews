package lasea.barebonesbrews.recipe;

import lasea.barebonesbrews.BareBonesBrews;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Recipe type + serializer for rough brewing in a vanilla cauldron. */
public final class ModRecipes {

    /** {@code barebonesbrews:rough_brewing}. */
    public static final ResourceLocation ROUGH_BREWING_ID =
            ResourceLocation.fromNamespaceAndPath(BareBonesBrews.MODID, "rough_brewing");

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, BareBonesBrews.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BareBonesBrews.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CauldronBrewingRecipe>> ROUGH_BREWING =
            RECIPE_TYPES.register("rough_brewing", () -> RecipeType.simple(ROUGH_BREWING_ID));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CauldronBrewingRecipe>> ROUGH_BREWING_SERIALIZER =
            RECIPE_SERIALIZERS.register("rough_brewing", CauldronBrewingRecipe.Serializer::new);

    private ModRecipes() {}
}
