package lasea.barebonesbrews.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import lasea.barebonesbrews.brewing.CauldronBrewBlockEntity;
import lasea.barebonesbrews.brewing.CauldronBrewing;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Renders only physical ingredients; the vanilla cauldron continues to render its own water. */
public final class CauldronBrewRenderer implements BlockEntityRenderer<CauldronBrewBlockEntity> {

    private static final float CENTER = 0.5F;
    private static final float ITEM_SCALE = 0.40F;
    private static final float BREW_ANGULAR_SPEED = 0.012F;
    private static final float ITEM_SINK_DELAY = 2.25F;
    private final ItemRenderer itemRenderer;

    public CauldronBrewRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CauldronBrewBlockEntity brew, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = brew.getLevel();
        List<ItemStack> ingredients = brew.floatingItems();
        int fillLevel = brew.fillLevel();
        if (level == null || fillLevel <= 0) {
            return;
        }

        float surface = (6.0F + fillLevel * 3.0F) / 16.0F;
        float time = level.getGameTime() + partialTick;
        if (ingredients.isEmpty()) {
            return;
        }
        int light = LevelRenderer.getLightColor(level, brew.getBlockPos().above());
        float completion = brew.isReady() ? brew.completionProgress(partialTick) : 0.0F;
        if (completion >= 1.0F) {
            return;
        }
        long positionSeed = brew.getBlockPos().asLong();

        int count = Math.min(ingredients.size(), CauldronBrewing.MAX_INGREDIENTS);
        for (int index = 0; index < count; index++) {
            ItemStack stack = ingredients.get(index);
            if (stack.isEmpty()) {
                continue;
            }

            long seed = mix(positionSeed, index);
            float baseAngle = Mth.TWO_PI * index / count + unit(seed) * 0.42F;
            float sink = delayedSink(completion, index);
            float smoothSink = sink * sink * (3.0F - 2.0F * sink);
            float dive = smoothSink * smoothSink;
            float angle = baseAngle + time * (brew.isBrewing() || brew.isReady()
                    ? BREW_ANGULAR_SPEED : 0.0015F) + smoothSink * 0.72F;
            float radius = count == 1 ? 0.08F : 0.19F + unit(seed >>> 12) * 0.035F;
            radius *= Mth.lerp(smoothSink, 1.0F, 0.18F);

            float bob = Mth.sin(time * (0.045F + unit(seed >>> 20) * 0.015F) + baseAngle)
                    * 0.009F * (1.0F - smoothSink);
            float drift = Mth.sin(time * 0.018F + baseAngle * 1.7F)
                    * 0.012F * (1.0F - smoothSink);
            float x = CENTER + Mth.cos(angle) * (radius + drift);
            float z = CENTER + Mth.sin(angle) * (radius + drift);
            float depthVariation = 0.025F * unit(seed >>> 48);
            float y = surface + 0.018F + bob - dive * (0.52F + depthVariation);
            float finalShrink = Mth.clamp((smoothSink - 0.72F) / 0.28F, 0.0F, 1.0F);
            float scale = ITEM_SCALE * Mth.lerp(finalShrink, 1.0F, 0.86F);

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(angle * Mth.RAD_TO_DEG + unit(seed >>> 32) * 80.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(88.0F + dive * 155.0F
                    + Mth.sin(time * 0.025F + baseAngle) * 3.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees((unit(seed >>> 40) - 0.5F) * 8.0F
                    + smoothSink * (unit(seed >>> 28) - 0.5F) * 70.0F));
            poseStack.scale(scale, scale, scale);
            itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, light,
                    OverlayTexture.NO_OVERLAY, poseStack, buffers, level, (int) seed);
            poseStack.popPose();
        }
    }

    private static long mix(long position, int index) {
        long value = position ^ (0x9E3779B97F4A7C15L * (index + 1L));
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        return value ^ value >>> 31;
    }

    private static float delayedSink(float completion, int index) {
        float elapsed = completion * CauldronBrewBlockEntity.COMPLETION_SINK_TICKS;
        float delay = index * ITEM_SINK_DELAY;
        return Mth.clamp((elapsed - delay)
                / (CauldronBrewBlockEntity.COMPLETION_SINK_TICKS - delay), 0.0F, 1.0F);
    }

    private static float unit(long value) {
        return (value & 0xFFFFL) / 65535.0F;
    }
}
