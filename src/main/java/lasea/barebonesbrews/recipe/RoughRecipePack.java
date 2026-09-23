package lasea.barebonesbrews.recipe;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.Config;
import lasea.barebonesbrews.brewing.BrewingMap;
import lasea.barebonesbrews.brewing.CauldronBrewing;
import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** A small in-memory datapack containing only the recipes valid for this mod set. */
public final class RoughRecipePack implements PackResources {

    public static final String PACK_ID = BareBonesBrews.MODID + ":generated_recipes";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> NAMESPACES = Set.of(BareBonesBrews.MODID);
    private static final String DESCRIPTION = "BareBonesBrews generated recipes";
    private static final ResourceLocation HEXALIA_RECIPE_TYPE =
            ResourceLocation.fromNamespaceAndPath("hexalia", "small_cauldron");
    private static final int HEXALIA_MAX_INGREDIENTS = 4;

    private final PackLocationInfo location = new PackLocationInfo(PACK_ID, Component.literal(DESCRIPTION),
            PackSource.BUILT_IN, Optional.of(new KnownPack(BareBonesBrews.MODID, PACK_ID, modVersion())));
    private volatile Map<String, byte[]> contents;

    private static String modVersion() {
        return ModList.get().getModContainerById(BareBonesBrews.MODID)
                .map(container -> container.getModInfo().getVersion().toString()).orElse("1");
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        byte[] data = contents().get(String.join("/", elements));
        return data == null ? null : () -> new ByteArrayInputStream(data);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != PackType.SERVER_DATA) {
            return null;
        }
        byte[] data = contents().get("data/" + id.getNamespace() + "/" + id.getPath());
        return data == null ? null : () -> new ByteArrayInputStream(data);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.SERVER_DATA || !NAMESPACES.contains(namespace)) {
            return;
        }
        String normalized = path.endsWith("/") ? path : path + "/";
        String prefix = "data/" + namespace + "/" + normalized;
        contents().forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                String resourcePath = key.substring(("data/" + namespace + "/").length());
                output.accept(ResourceLocation.fromNamespaceAndPath(namespace, resourcePath),
                        () -> new ByteArrayInputStream(value));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? NAMESPACES : Set.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        return serializer == PackMetadataSection.TYPE
                ? (T) new PackMetadataSection(Component.literal(DESCRIPTION), packFormat(), Optional.empty())
                : null;
    }

    @Override
    public PackLocationInfo location() {
        return location;
    }

    @Override
    public void close() {}

    private Map<String, byte[]> contents() {
        Map<String, byte[]> local = contents;
        if (local == null) {
            synchronized (this) {
                if (contents == null) {
                    contents = build();
                }
                local = contents;
            }
        }
        return local;
    }

    private Map<String, byte[]> build() {
        Map<String, byte[]> generated = new TreeMap<>();
        generated.put("pack.mcmeta", json(packMeta()));
        if (!Config.roughPotionsEnabled() || !Config.cauldronRecipesEnabled()) {
            return Map.copyOf(generated);
        }

        boolean useHexalia = ModList.get().isLoaded("hexalia");
        if (!useHexalia) {
            var source = Potions.AWKWARD;
            ResourceLocation sourceId = RoughPotionFactory.idOf(source);
            if (sourceId != null && RoughPotionFactory.isBrewable(source)) {
                ItemStack result = RoughPotionFactory.create(source);
                ResourceLocation recipeId = RoughPotionFactory.recipeId(sourceId);
                String path = "data/" + BareBonesBrews.MODID + "/recipe/"
                        + recipeId.getPath() + ".json";
                generated.put(path, json(recipeJson(baseIngredient(), result,
                        RoughRecipeBuilder.durationFor(source))));
            }
        }
        buildPotionRecipes(generated, useHexalia);

        return Map.copyOf(generated);
    }

    private static void buildPotionRecipes(Map<String, byte[]> generated, boolean useHexalia) {
        Set<String> ingredientSets = new HashSet<>();
        Ingredient gunpowder = Ingredient.of(Items.GUNPOWDER);
        for (BrewingMap.BrewingPath path : BrewingMap.pathsFromRoughBase()) {
            addPotionRecipe(generated, ingredientSets, useHexalia, "potion", path.result(),
                    path.ingredients(),
                    RoughRecipeBuilder.durationFor(path.result()));

            List<Ingredient> splashIngredients = new java.util.ArrayList<>(path.ingredients());
            splashIngredients.add(gunpowder);
            addPotionRecipe(generated, ingredientSets, useHexalia, "splash", path.result(),
                    splashIngredients, RoughRecipeBuilder.durationFor(path.result()));

            List<Ingredient> lingeringIngredients = new java.util.ArrayList<>(splashIngredients);
            lingeringIngredients.add(Ingredient.of(Items.DRAGON_BREATH));
            addPotionRecipe(generated, ingredientSets, useHexalia, "lingering", path.result(),
                    lingeringIngredients, RoughRecipeBuilder.durationFor(path.result()));
        }
    }

    private static void addPotionRecipe(Map<String, byte[]> generated,
            Set<String> ingredientSets, boolean useHexalia, String form, Holder<Potion> potion,
            List<Ingredient> potionIngredients, int duration) {
        ResourceLocation potionId = RoughPotionFactory.idOf(potion);
        int ingredientCount = potionIngredients.size() + (useHexalia ? 0 : 1);
        int capacity = useHexalia ? HEXALIA_MAX_INGREDIENTS : CauldronBrewing.MAX_INGREDIENTS;
        if (potionId == null || ingredientCount == 0 || ingredientCount > capacity) {
            return;
        }
        JsonArray ingredients = useHexalia ? new JsonArray() : baseIngredient();
        for (Ingredient ingredient : potionIngredients) {
            ingredients.add(Ingredient.CODEC_NONEMPTY.encodeStart(JsonOps.INSTANCE, ingredient)
                    .getOrThrow());
        }
        String signature = ingredientSignature(ingredients);
        if (!ingredientSets.add(signature)) {
            return;
        }

        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(BareBonesBrews.MODID,
                (useHexalia ? "hexalia_rough/" : "rough_brewing/")
                        + form + "/" + potionId.getNamespace() + "/" + potionId.getPath());
        String resourcePath = "data/" + BareBonesBrews.MODID + "/recipe/"
                + recipeId.getPath() + ".json";
        ItemStack result = switch (form) {
            case "splash" -> RoughPotionFactory.createSplash(potion);
            case "lingering" -> RoughPotionFactory.createLingering(potion);
            default -> RoughPotionFactory.create(potion);
        };
        JsonObject recipe = useHexalia
                ? hexaliaRecipeJson(ingredients, result, duration)
                : recipeJson(ingredients, result, duration);
        generated.put(resourcePath, json(recipe));
    }

    private static JsonObject hexaliaRecipeJson(JsonArray ingredients, ItemStack result, int duration) {
        JsonObject json = recipeJson(ingredients, result, duration);
        json.addProperty("type", HEXALIA_RECIPE_TYPE.toString());
        return json;
    }

    private static String ingredientSignature(JsonArray ingredients) {
        return java.util.stream.StreamSupport.stream(ingredients.spliterator(), false)
                .map(Object::toString)
                .sorted()
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private static JsonObject recipeJson(JsonArray ingredients, ItemStack result, int duration) {
        JsonObject json = new JsonObject();
        json.addProperty("type", ModRecipes.ROUGH_BREWING_ID.toString());
        json.add("ingredients", ingredients);
        json.add("result", ItemStack.CODEC.encodeStart(
                RegistryOps.create(JsonOps.INSTANCE, registryAccess()), result).getOrThrow());
        json.addProperty("duration", duration);
        return json;
    }

    private static JsonArray baseIngredient() {
        JsonArray ingredients = new JsonArray();
        JsonObject base = new JsonObject();
        base.addProperty("tag", ModItems.ROUGH_BASE_TAG.location().toString());
        ingredients.add(base);
        return ingredients;
    }

    private static HolderLookup.Provider registryAccess() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? RegistryAccess.EMPTY : server.registryAccess();
    }

    private static JsonObject packMeta() {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("description", DESCRIPTION);
        pack.addProperty("pack_format", packFormat());
        root.add("pack", pack);
        return root;
    }

    private static int packFormat() {
        return SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA);
    }

    private static byte[] json(JsonObject object) {
        return GSON.toJson(object).getBytes(StandardCharsets.UTF_8);
    }
}
