package lasea.barebonesbrews.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.neoforged.fml.loading.FMLLoader;

/** Selects exactly one cauldron integration before either target class is transformed. */
public final class BareBonesBrewsMixinPlugin implements IMixinConfigPlugin {

    private static final String VANILLA_CAULDRON_MIXIN = ".CauldronEntityBlockMixin";
    private static final String HEXALIA_CAULDRON_MIXIN = ".HexaliaSmallCauldronContentsMixin";
    private boolean hexaliaLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        hexaliaLoaded = FMLLoader.getLoadingModList().getModFileById("hexalia") != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(VANILLA_CAULDRON_MIXIN)) {
            return !hexaliaLoaded;
        }
        if (mixinClassName.endsWith(HEXALIA_CAULDRON_MIXIN)) {
            return hexaliaLoaded;
        }
        return true;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass,
            String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass,
            String mixinClassName, IMixinInfo mixinInfo) {}
}
