package lasea.barebonesbrews.component;

import lasea.barebonesbrews.BareBonesBrews;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Persistent identity that connects a rough stack back to its source potion. */
public final class ModComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BareBonesBrews.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SOURCE_POTION =
            COMPONENT_TYPES.register("source_potion", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    private ModComponents() {}
}
