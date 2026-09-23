package lasea.barebonesbrews.compat.jade;

import lasea.barebonesbrews.brewing.CauldronBrewBlockEntity;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;

/** Loaded by Jade only when Jade itself is present. */
@WailaPlugin
public final class BareBonesBrewsJadePlugin implements IWailaPlugin {

    private static final String HEXALIA_BLOCK =
            "net.astralya.hexalia.block.custom.SmallCauldronBlock";
    private static final String HEXALIA_BLOCK_ENTITY =
            "net.astralya.hexalia.block.entity.custom.SmallCauldronBlockEntity";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(CauldronJadeProvider.INSTANCE,
                CauldronBrewBlockEntity.class);
        optionalClass(HEXALIA_BLOCK_ENTITY).ifPresent(type ->
                registration.registerBlockDataProvider(CauldronJadeProvider.INSTANCE, type));
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(CauldronJadeProvider.INSTANCE,
                AbstractCauldronBlock.class);
        optionalClass(HEXALIA_BLOCK).filter(Block.class::isAssignableFrom).ifPresent(type -> {
            @SuppressWarnings("unchecked")
            Class<? extends Block> blockType = (Class<? extends Block>) type;
            registration.registerBlockComponent(CauldronJadeProvider.INSTANCE, blockType);
        });
        registration.addTooltipCollectedCallback((box, accessor) -> {
            if (accessor instanceof BlockAccessor blockAccessor
                    && !(blockAccessor.getBlockEntity() instanceof CauldronBrewBlockEntity)
                    && CauldronJadeProvider.hasBrewingData(blockAccessor)) {
                box.getTooltip().remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
            }
        });
    }

    private static java.util.Optional<Class<?>> optionalClass(String name) {
        try {
            return java.util.Optional.of(Class.forName(name, false,
                    BareBonesBrewsJadePlugin.class.getClassLoader()));
        } catch (ClassNotFoundException exception) {
            return java.util.Optional.empty();
        }
    }
}
