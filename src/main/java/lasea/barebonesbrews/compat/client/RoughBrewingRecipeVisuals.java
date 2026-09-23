package lasea.barebonesbrews.compat.client;

import java.util.List;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import lasea.barebonesbrews.brewing.CauldronBrewing;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.data.ModelData;

/** A block-space scene and layout shared by JEI, REI and EMI. */
public final class RoughBrewingRecipeVisuals {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 142;
    public static final int OUTPUT_X = 155;
    public static final int OUTPUT_Y = 53;
    public static final int ARROW_X = 127;
    public static final int ARROW_Y = 53;
    public static final int DURATION_Y = 130;
    // A regular pentagon has equally spaced vertices and a diagonal/side ratio of phi.
    // Pixel rounding keeps the 18px slots crisp; 50px radius leaves the scene unobstructed.
    public static final int[][] INPUT_SLOTS = createInputSlots();

    private static int[][] createInputSlots() {
        int count = CauldronBrewing.MAX_INGREDIENTS;
        int[][] slots = new int[count][2];
        for (int index = 0; index < count; index++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * index / count;
            slots[index][0] = (int) Math.round(63 + 50 * Math.cos(angle)) - 9;
            slots[index][1] = (int) Math.round(62 + 50 * Math.sin(angle)) - 9;
        }
        return slots;
    }

    private static final int WATER_COLOR = 0x3F76E4;
    private static final ResourceLocation WATER_STILL =
            ResourceLocation.withDefaultNamespace("block/water_still");
    private static final BlockState CAULDRON = Blocks.CAULDRON.defaultBlockState();
    private static final BlockState HEAT_SOURCE = Blocks.CAMPFIRE.defaultBlockState()
            .setValue(BlockStateProperties.LIT, true);
    private static final float LIQUID_MIN = 2.01F / 16.0F;
    private static final float LIQUID_MAX = 13.99F / 16.0F;
    private static final float LIQUID_BOTTOM = 4.01F / 16.0F;

    private RoughBrewingRecipeVisuals() {
    }

    public static void draw(GuiGraphics graphics, int originX, int originY,
            List<Ingredient> ingredients, ItemStack output) {
        Minecraft minecraft = Minecraft.getInstance();
        // Small, monotonic values retain sub-tick precision even with long-running worlds.
        float time = (Util.getMillis() % 240000L) / 50.0F;
        float cycle = time % 200.0F;
        float sink = smoothStep((cycle - 90.0F) / 35.0F);
        float surface = 13.5F / 16.0F + Mth.sin(time * 0.10F) * 0.006F;
        PotionContents potion = output.get(DataComponents.POTION_CONTENTS);
        int resultColor = potion == null ? WATER_COLOR : potion.getColor();
        float mixed = smoothStep(cycle / 125.0F) * (1.0F - smoothStep((cycle - 185.0F) / 15.0F));

        // GuiGraphics.flush() disables depth testing. End 3D batches directly instead.
        graphics.flush();
        PoseStack pose = graphics.pose();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        pose.pushPose();
        try {
            // Scene vertices stay near z=100, below GUI items (150) and tooltips (400).
            pose.translate(originX + 63.0F, originY + 68.0F, 100.0F);
            pose.scale(28.0F, -28.0F, 28.0F);
            pose.mulPose(Axis.XP.rotationDegrees(25.0F));
            pose.mulPose(Axis.YP.rotationDegrees(35.0F));
            pose.translate(-0.5F, 0.0F, -0.5F);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            Lighting.setupFor3DItems();

            renderBlock(minecraft, pose, buffers, CAULDRON);
            pose.pushPose();
            try {
                // Campfire logs are seven pixels high: their top supports the cauldron.
                pose.translate(0.0F, -7.0F / 16.0F, 0.0F);
                renderBlock(minecraft, pose, buffers, HEAT_SOURCE);
            } finally {
                pose.popPose();
            }
            renderIngredients(minecraft, pose, buffers, ingredients, time, cycle, sink, surface);
            buffers.endBatch();

            // Translucent water follows opaque geometry: the rim occludes it, while
            // submerged ingredients remain visible through the liquid surface.
            renderLiquid(minecraft, pose, buffers, surface, blend(WATER_COLOR, resultColor, mixed));
            buffers.endBatch();
        } finally {
            // Submit all scene vertices before any later tooltip rendering.
            buffers.endBatch();
            pose.popPose();
            Lighting.setupFor3DItems();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private static void renderBlock(Minecraft minecraft, PoseStack pose,
            MultiBufferSource buffers, BlockState state) {
        minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
    }

    private static void renderIngredients(Minecraft minecraft, PoseStack pose,
            MultiBufferSource buffers, List<Ingredient> ingredients,
            float time, float cycle, float sink, float surface) {
        if (cycle >= 125.0F) {
            return;
        }
        int count = Math.min(INPUT_SLOTS.length, ingredients.size());
        float appear = smoothStep(cycle / 10.0F);
        if (appear <= 0.0F) {
            return;
        }
        for (int index = 0; index < count; index++) {
            ItemStack[] alternatives = ingredients.get(index).getItems();
            if (alternatives.length == 0) {
                continue;
            }
            ItemStack stack = alternatives[(int) (time / 40.0F) % alternatives.length];
            float angle = Mth.TWO_PI * index / count + time * 0.035F + sink * 1.8F;
            float radius = (count == 1 ? 0.08F : 0.22F) * (1.0F - sink * 0.8F);
            float bob = Mth.sin(time * 0.09F + index * 2.0F) * 0.015F;
            pose.pushPose();
            try {
                pose.translate(0.5F + Mth.cos(angle) * radius,
                        surface + 0.035F + bob - sink * 0.48F,
                        0.5F + Mth.sin(angle) * radius);
                pose.mulPose(Axis.YP.rotationDegrees(angle * Mth.RAD_TO_DEG));
                pose.mulPose(Axis.XP.rotationDegrees(78.0F + Mth.sin(time * 0.06F + index) * 7.0F));
                pose.mulPose(Axis.ZP.rotationDegrees(Mth.cos(time * 0.05F + index) * 6.0F));
                float scale = 0.36F * appear * (1.0F - sink * 0.3F);
                pose.scale(scale, scale, scale);
                minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        pose, buffers, minecraft.level, index);
            } finally {
                pose.popPose();
            }
        }
    }

    private static void renderLiquid(Minecraft minecraft, PoseStack pose,
            MultiBufferSource buffers, float top, int rgb) {
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER_STILL);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        float a = LIQUID_MIN;
        float b = LIQUID_MAX;
        float bottom = LIQUID_BOTTOM;
        int color = 0xD8000000 | rgb;
        // Six textured faces form a liquid volume inside the actual cauldron cavity.
        quad(vertices, pose.last(), sprite, color, 0, 1, 0,
                a, top, a, a, top, b, b, top, b, b, top, a);
        quad(vertices, pose.last(), sprite, color, 0, 0, -1,
                b, top, a, b, bottom, a, a, bottom, a, a, top, a);
        quad(vertices, pose.last(), sprite, color, 0, 0, 1,
                a, top, b, a, bottom, b, b, bottom, b, b, top, b);
        quad(vertices, pose.last(), sprite, color, -1, 0, 0,
                a, top, a, a, bottom, a, a, bottom, b, a, top, b);
        quad(vertices, pose.last(), sprite, color, 1, 0, 0,
                b, top, b, b, bottom, b, b, bottom, a, b, top, a);
        quad(vertices, pose.last(), sprite, color, 0, -1, 0,
                a, bottom, b, a, bottom, a, b, bottom, a, b, bottom, b);
    }

    private static void quad(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite,
            int color, float nx, float ny, float nz,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3) {
        vertex(vertices, pose, color, x0, y0, z0, sprite.getU0(), sprite.getV0(), nx, ny, nz);
        vertex(vertices, pose, color, x1, y1, z1, sprite.getU0(), sprite.getV1(), nx, ny, nz);
        vertex(vertices, pose, color, x2, y2, z2, sprite.getU1(), sprite.getV1(), nx, ny, nz);
        vertex(vertices, pose, color, x3, y3, z3, sprite.getU1(), sprite.getV0(), nx, ny, nz);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, int color,
            float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        vertices.addVertex(pose, x, y, z).setColor(color).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static int blend(int from, int to, float amount) {
        return (Mth.lerpInt(amount, (from >> 16) & 255, (to >> 16) & 255) << 16)
                | (Mth.lerpInt(amount, (from >> 8) & 255, (to >> 8) & 255) << 8)
                | Mth.lerpInt(amount, from & 255, to & 255);
    }
}
