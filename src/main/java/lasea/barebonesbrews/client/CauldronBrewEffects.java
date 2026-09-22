package lasea.barebonesbrews.client;

import java.util.Map;
import java.util.WeakHashMap;

import lasea.barebonesbrews.brewing.CauldronBrewBlockEntity;
import lasea.barebonesbrews.brewing.CauldronBrewing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Client-only, state-aware cauldron particles. Vanilla remains responsible for the water surface. */
public final class CauldronBrewEffects {

    private static final Map<CauldronBrewBlockEntity, VisualState> STATES = new WeakHashMap<>();

    private CauldronBrewEffects() {}

    public static void tick(Level level, BlockPos pos, CauldronBrewBlockEntity brew) {
        if (!brew.hasBrew() || brew.fillLevel() <= 0) {
            STATES.remove(brew);
            return;
        }

        VisualState state = STATES.computeIfAbsent(brew, ignored -> new VisualState());
        if (brew.isReady() && !state.ready) {
            refreshLiquidTint(level, pos);
            completionBurst(level, pos, brew);
        }
        state.ready = brew.isReady();

        if (!CauldronBrewing.isHeated(level, pos)) {
            return;
        }

        RandomSource random = level.random;
        double surface = surfaceY(pos, brew.fillLevel());
        if (brew.isBrewing()) {
            if (random.nextFloat() < 0.22F) {
                bubble(level, pos, surface, random);
            }
            if (brew.brewColor() != 0 && random.nextFloat() < 0.09F) {
                coloredMote(level, pos, surface, brew.brewColor(), random, false);
            }
            if (random.nextFloat() < 0.045F) {
                level.addParticle(ParticleTypes.WHITE_SMOKE,
                        center(pos.getX(), random, 0.24), surface + 0.08,
                        center(pos.getZ(), random, 0.24), 0.0, 0.012, 0.0);
            }
        } else if (brew.isReady()) {
            float sink = brew.completionProgress(0.0F);
            if (sink < 1.0F && random.nextFloat() < 0.42F) {
                sinkingBubble(level, pos, surface, sink, random);
            }
            if (random.nextFloat() < 0.11F) {
                coloredMote(level, pos, surface, brew.brewColor(), random, true);
            }
            if (random.nextFloat() < 0.07F) {
                bubble(level, pos, surface, random);
            }
        } else if (random.nextFloat() < 0.035F) {
            level.addParticle(ParticleTypes.WHITE_SMOKE,
                    center(pos.getX(), random, 0.18), surface + 0.05,
                    center(pos.getZ(), random, 0.18), 0.0, 0.008, 0.0);
        }
    }

    private static void completionBurst(Level level, BlockPos pos, CauldronBrewBlockEntity brew) {
        RandomSource random = level.random;
        double surface = surfaceY(pos, brew.fillLevel());
        for (int i = 0; i < 12; i++) {
            double angle = Mth.TWO_PI * i / 12.0 + random.nextDouble() * 0.12;
            double speed = 0.009 + random.nextDouble() * 0.009;
            level.addParticle(color(brew.brewColor()),
                    pos.getX() + 0.5 + Mth.cos((float) angle) * 0.27,
                    surface + 0.018 + random.nextDouble() * 0.025,
                    pos.getZ() + 0.5 + Mth.sin((float) angle) * 0.27,
                    -Mth.cos((float) angle) * speed, 0.008 + random.nextDouble() * 0.008,
                    -Mth.sin((float) angle) * speed);
        }
        for (int i = 0; i < 7; i++) {
            level.addParticle(ParticleTypes.BUBBLE_POP,
                    center(pos.getX(), random, 0.24), surface + 0.015,
                    center(pos.getZ(), random, 0.24), 0.0, 0.012, 0.0);
        }
    }

    private static void refreshLiquidTint(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
    }

    private static void sinkingBubble(Level level, BlockPos pos, double surface, float sink,
            RandomSource random) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double radius = 0.08 + random.nextDouble() * 0.16 * (1.0 - sink * 0.55);
        level.addParticle(ParticleTypes.BUBBLE,
                pos.getX() + 0.5 + Mth.cos((float) angle) * radius,
                surface - 0.035 - sink * 0.32,
                pos.getZ() + 0.5 + Mth.sin((float) angle) * radius,
                0.0, 0.012 + random.nextDouble() * 0.008, 0.0);
    }

    private static void bubble(Level level, BlockPos pos, double surface, RandomSource random) {
        level.addParticle(ParticleTypes.BUBBLE_POP,
                center(pos.getX(), random, 0.27), surface + 0.018,
                center(pos.getZ(), random, 0.27), 0.0, 0.015, 0.0);
    }

    private static void coloredMote(Level level, BlockPos pos, double surface, int rgb,
            RandomSource random, boolean ready) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double radius = ready ? 0.18 : 0.25;
        double tangent = ready ? 0.006 : 0.012;
        level.addParticle(color(rgb),
                pos.getX() + 0.5 + Mth.cos((float) angle) * radius,
                surface + 0.035,
                pos.getZ() + 0.5 + Mth.sin((float) angle) * radius,
                -Mth.sin((float) angle) * tangent, ready ? 0.018 : 0.012,
                Mth.cos((float) angle) * tangent);
    }

    private static ColorParticleOption color(int rgb) {
        int safe = rgb == 0 ? 0xB8D8E8 : rgb;
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, FastColor.ARGB32.opaque(safe));
    }

    private static double surfaceY(BlockPos pos, int fillLevel) {
        return pos.getY() + (6.0 + fillLevel * 3.0) / 16.0;
    }

    private static double center(int coordinate, RandomSource random, double radius) {
        return coordinate + 0.5 + (random.nextDouble() - 0.5) * radius * 2.0;
    }

    private static final class VisualState {
        private boolean ready;
    }
}
