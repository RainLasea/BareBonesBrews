package lasea.barebonesbrews;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client entry point. The only client-specific behaviour is the item tint, which is registered by
 * {@link lasea.barebonesbrews.client.PotionItemColors}; this class just wires up the config screen.
 */
@Mod(value = BareBonesBrews.MODID, dist = Dist.CLIENT)
public class BareBonesBrewsClient {

    public BareBonesBrewsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
