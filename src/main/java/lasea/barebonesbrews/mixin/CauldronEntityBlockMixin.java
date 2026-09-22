package lasea.barebonesbrews.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import lasea.barebonesbrews.brewing.CauldronBrewBlockEntity;
import lasea.barebonesbrews.brewing.ModBrewing;
import lasea.barebonesbrews.client.CauldronBrewEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gives vanilla cauldrons a block entity.
 *
 * <p>A cauldron's own state is only its fill level. The injected block entity owns the complete brew
 * state and synchronizes the small visual subset to clients.
 *
 * <p>The mixin applies to every cauldron, but nothing changes for cauldrons only used for water: the
 * block entity starts empty and the ticker does nothing until a brew exists.
 */
@Mixin(AbstractCauldronBlock.class)
public abstract class CauldronEntityBlockMixin implements EntityBlock {

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.is(net.minecraft.world.level.block.Blocks.CAULDRON)
                || state.is(net.minecraft.world.level.block.Blocks.WATER_CAULDRON)
                || state.is(net.minecraft.world.level.block.Blocks.LAVA_CAULDRON)
                || state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW_CAULDRON)) {
            return new CauldronBrewBlockEntity(pos, state);
        }
        return null;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (type != ModBrewing.CAULDRON_BLOCK_ENTITY.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) barebonesbrews$cauldronTicker();
    }

    @Unique
    private static BlockEntityTicker<CauldronBrewBlockEntity> barebonesbrews$cauldronTicker() {
        return (level, pos, state, be) -> {
            if (level.isClientSide) {
                CauldronBrewEffects.tick(level, pos, be);
            } else if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                be.serverTick(serverLevel);
            }
        };
    }
}
