package lasea.barebonesbrews.brewing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Restrained, event-driven audio cues for the brew lifecycle. */
final class CauldronBrewSoundscape {

    private CauldronBrewSoundscape() {}

    static void ingredientAdded(ServerLevel level, BlockPos pos) {
        play(level, pos, SoundEvents.FISHING_BOBBER_SPLASH, 0.22F, 1.35F);
        play(level, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.14F, 1.15F);
    }

    static void brewingStarted(ServerLevel level, BlockPos pos) {
        play(level, pos, SoundEvents.AMBIENT_UNDERWATER_ENTER, 0.32F, 1.25F);
        play(level, pos, SoundEvents.BREWING_STAND_BREW, 0.28F, 1.18F);
    }

    static void brewingTick(ServerLevel level, BlockPos pos, int progress) {
        int offset = (int) (pos.asLong() & 31L);
        if ((progress + offset) % 64 == 0) {
            float pitch = 0.92F + level.random.nextFloat() * 0.22F;
            play(level, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.10F, pitch);
        }
    }

    static void bottled(ServerLevel level, BlockPos pos) {
        play(level, pos, SoundEvents.BOTTLE_FILL, 0.8F, 1.08F);
    }

    private static void play(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }
}
