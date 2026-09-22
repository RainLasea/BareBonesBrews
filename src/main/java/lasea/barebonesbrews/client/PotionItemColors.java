package lasea.barebonesbrews.client;

import lasea.barebonesbrews.brewing.ModBrewing;
import lasea.barebonesbrews.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Client-side wiring for the generated potion items: their tint, and a name-resolution check.
 *
 * <p>Vanilla only registers a potion tint for {@code minecraft:potion}, {@code splash_potion} and
 * {@code lingering_potion}, so our own potion items would otherwise render as a flat white overlay.
 * Layer 0 is the vanilla potion overlay, layer 1 the bottle, layer 2 the grit decal, which is left
 * untinted on purpose.
 */
@EventBusSubscriber(modid = lasea.barebonesbrews.BareBonesBrews.MODID, value = Dist.CLIENT)
public final class PotionItemColors {

    private static final int DEFAULT_WATER_COLOR = 0x3F76E4;

    private static final ItemColor POTION_TINT = (stack, tintIndex) -> {
        if (tintIndex != 0) {
            return -1;
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return FastColor.ARGB32.opaque(contents.getColor());
    };

    private static final BlockColor CAULDRON_WATER_TINT = (state, level, pos, tintIndex) -> {
        if (tintIndex != 0) {
            return -1;
        }
        if (level != null && pos != null
                && level.getBlockEntity(pos) instanceof lasea.barebonesbrews.brewing.CauldronBrewBlockEntity brew
                && brew.isReady() && brew.brewColor() != 0) {
            return brew.brewColor();
        }
        return level != null && pos != null
                ? BiomeColors.getAverageWaterColor(level, pos)
                : DEFAULT_WATER_COLOR;
    };

    private PotionItemColors() {}

    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(POTION_TINT, ModItems.ROUGH_POTION.get());
    }

    /** Tints the vanilla cauldron model's existing water face; no second liquid surface is drawn. */
    @SubscribeEvent
    static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(CAULDRON_WATER_TINT, Blocks.WATER_CAULDRON);
    }

    /** Registers the ingredient-only renderer. The baked vanilla model renders the tinted water. */
    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBrewing.CAULDRON_BLOCK_ENTITY.get(), CauldronBrewRenderer::new);
    }

}
