package lasea.barebonesbrews.client;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.fluid.ModFluids;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidStack;

@EventBusSubscriber(modid = BareBonesBrews.MODID, value = Dist.CLIENT)
public final class PotionFluidClient {
    private PotionFluidClient() {}

    @SubscribeEvent
    static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_still");
            }
            @Override public ResourceLocation getFlowingTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_flow");
            }
            @Override public int getTintColor(FluidStack stack) {
                return 0xFF000000 | stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
            }
        }, ModFluids.POTION_TYPE.get());
    }
}
