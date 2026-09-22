package lasea.barebonesbrews.recipe;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.Config;
import lasea.barebonesbrews.brewing.BrewingMap;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import net.minecraft.SharedConstants;
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

        Map<ResourceLocation, Ingredient> selected =
                RoughRecipeBuilder.selectIngredients(BrewingMap.snapshot());
        boolean useHexalia = ModList.get().isLoaded("hexalia");
        BuiltInPotions.sorted().forEach(source -> {
            ResourceLocation sourceId = RoughPotionFactory.idOf(source);
            Ingredient reagent = sourceId == null ? null : selected.get(sourceId);
            if (reagent == null) {
                return;
            }
            ItemStack result = RoughPotionFactory.create(source);
            ResourceLocation recipeId = useHexalia
                    ? hexaliaRecipeId(sourceId) : RoughPotionFactory.recipeId(sourceId);
            String path = "data/" + BareBonesBrews.MODID + "/recipe/" + recipeId.getPath() + ".json";
            generated.put(path, json(useHexalia
                    ? hexaliaRecipeJson(reagent, RoughRecipeBuilder.durationFor(source))
                    : recipeJson(result, reagent, RoughRecipeBuilder.durationFor(source))));
        });

        Map<String, byte[]> result = Map.copyOf(generated);
        BareBonesBrews.LOGGER.info("Generated {} unambiguous cauldron recipes", result.size() - 1);
        return result;
    }

    private static ResourceLocation hexaliaRecipeId(ResourceLocation sourceId) {
        return ResourceLocation.fromNamespaceAndPath(BareBonesBrews.MODID,
                "hexalia_rough/" + sourceId.getNamespace() + "/" + sourceId.getPath());
    }

    private static JsonObject hexaliaRecipeJson(Ingredient reagent, int duration) {
        JsonObject json = new JsonObject();
        json.addProperty("type", HEXALIA_RECIPE_TYPE.toString());
        json.add("ingredients", ingredients(reagent));
        JsonObject result = new JsonObject();
        result.addProperty("item", BareBonesBrews.MODID + ":rough_potion");
        json.add("result", result);
        json.addProperty("duration", duration);
        return json;
    }

    private static JsonObject recipeJson(ItemStack result, Ingredient reagent, int duration) {
        JsonObject json = new JsonObject();
        json.addProperty("type", ModRecipes.ROUGH_BREWING_ID.toString());
        json.add("ingredients", ingredients(reagent));
        json.add("result", ItemStack.CODEC.encodeStart(
                RegistryOps.create(JsonOps.INSTANCE, registryAccess()), result).getOrThrow());
        json.addProperty("duration", duration);
        return json;
    }

    private static JsonArray ingredients(Ingredient reagent) {
        JsonArray ingredients = new JsonArray();
        JsonObject base = new JsonObject();
        base.addProperty("tag", BareBonesBrews.MODID + ":rough_base");
        ingredients.add(base);
        ingredients.add(Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, reagent).getOrThrow());
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

    private static final class BuiltInPotions {
        private static java.util.stream.Stream<net.minecraft.core.Holder.Reference<net.minecraft.world.item.alchemy.Potion>> sorted() {
            return net.minecraft.core.registries.BuiltInRegistries.POTION.holders()
                    .filter(RoughPotionFactory::isEligible)
                    .sorted(Comparator.comparing(holder -> holder.key().location()));
        }
    }
}
