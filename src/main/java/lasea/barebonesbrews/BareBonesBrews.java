package lasea.barebonesbrews;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import lasea.barebonesbrews.brewing.ModBrewing;
import lasea.barebonesbrews.component.ModComponents;
import lasea.barebonesbrews.item.ModItems;
import lasea.barebonesbrews.potion.RoughPotionFactory;
import lasea.barebonesbrews.recipe.ModRecipes;
import lasea.barebonesbrews.recipe.RoughRecipePack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * BareBonesBrews — dynamic rough-potion compatibility for whatever potions a pack happens to have.
 *
 * <p>Rough potions are data-bearing variants of one item, just like vanilla potions. This avoids
 * mutating registries after startup and lets configuration be applied when stacks are created.
 */
@Mod(BareBonesBrews.MODID)
public class BareBonesBrews {

    public static final String MODID = "barebonesbrews";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BareBonesBrews(IEventBus modEventBus, ModContainer modContainer) {
        ModComponents.COMPONENT_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModBrewing.BLOCK_ENTITIES.register(modEventBus);

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onBuildCreativeTab);
        modEventBus.addListener(this::onAddPackFinders);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(lasea.barebonesbrews.brewing.BrewingMap::bootstrap);
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        ModItems.addToCreativeTabs(event);
    }

    // ------------------------------------------------------------------ recipe injection

    /**
     * Hands the game a virtual datapack holding the generated brewing recipes.
     *
     * <p>Registry mutation is deliberately absent here. This event also fires on the client while
     * picking a world, which is long after every registry has been frozen - registering at this point
     * threw {@code Registry is already frozen} and took the whole game down. This method only reads.
     */
    private void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        RoughRecipePack pack = new RoughRecipePack();

        event.addRepositorySource(consumer -> consumer.accept(Pack.readMetaAndCreate(
                pack.location(),
                new Pack.ResourcesSupplier() {
                    @Override
                    public net.minecraft.server.packs.PackResources openPrimary(
                            net.minecraft.server.packs.PackLocationInfo location) {
                        return pack;
                    }

                    @Override
                    public net.minecraft.server.packs.PackResources openFull(
                            net.minecraft.server.packs.PackLocationInfo location, Pack.Metadata metadata) {
                        return pack;
                    }
                },
                PackType.SERVER_DATA,
                new PackSelectionConfig(true, Pack.Position.TOP, true))));
    }

    private void onServerStarted(ServerStartedEvent event) {
        PotionBrewing brewing = event.getServer().potionBrewing();
        if (brewing != null) {
            lasea.barebonesbrews.brewing.BrewingMap.capture(brewing);
        } else {
            LOGGER.warn("[BareBonesBrews] Server has no brewing table; brewing recipes may be incomplete.");
        }
    }

    // ------------------------------------------------------------------ diagnostics

    /** Prints a short summary in chat, so the mod is self-explanatory. */
    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.showSummaryMessage()) {
            return;
        }
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.barebonesbrews.summary",
                RoughPotionFactory.eligiblePotionCount(),
                lasea.barebonesbrews.brewing.BrewingMap.size()), false);
    }
}
