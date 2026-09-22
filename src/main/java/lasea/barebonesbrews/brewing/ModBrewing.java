package lasea.barebonesbrews.brewing;

import lasea.barebonesbrews.BareBonesBrews;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The block entity type backing rough brewing on a vanilla cauldron.
 *
 * <p>Registered for the vanilla cauldron blocks rather than a block of our own; the block only gains
 * block-entity support because {@code CauldronEntityBlockMixin} adds {@code EntityBlock} to
 * {@code AbstractCauldronBlock}.
 */
public final class ModBrewing {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BareBonesBrews.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CauldronBrewBlockEntity>> CAULDRON_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("cauldron_brew", () -> BlockEntityType.Builder
                    // Every vanilla cauldron block can receive the EntityBlock mixin.
                    .of(CauldronBrewBlockEntity::new,
                            Blocks.CAULDRON, Blocks.WATER_CAULDRON,
                            Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON)
                    .build(null));

    private ModBrewing() {}
}
