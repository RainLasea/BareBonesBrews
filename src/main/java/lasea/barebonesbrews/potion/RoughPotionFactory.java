package lasea.barebonesbrews.potion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.Config;
import lasea.barebonesbrews.component.ModComponents;
import lasea.barebonesbrews.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

/** Creates configured rough-potion stacks without adding synthetic registry entries. */
public final class RoughPotionFactory {

    private RoughPotionFactory() {}

    public static List<ItemStack> allStacks() {
        if (!Config.roughPotionsEnabled()) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        BuiltInRegistries.POTION.holders()
                .sorted(Comparator.comparing(holder -> holder.unwrapKey().orElseThrow().location()))
                .filter(RoughPotionFactory::isEligible)
                .map(RoughPotionFactory::create)
                .forEach(stacks::add);
        return List.copyOf(stacks);
    }

    public static int eligiblePotionCount() {
        return allStacks().size();
    }

    public static boolean isEligible(Holder<Potion> source) {
        ResourceLocation id = idOf(source);
        return id != null
                && !source.value().getEffects().isEmpty()
                && !Config.isNamespaceExcluded(id.getNamespace());
    }

    public static ItemStack create(Holder<Potion> source) {
        ResourceLocation sourceId = idOf(source);
        if (sourceId == null) {
            return ItemStack.EMPTY;
        }
        List<MobEffectInstance> effects = source.value().getEffects().stream()
                .map(RoughPotionFactory::dilute)
                .toList();
        ItemStack stack = new ItemStack(ModItems.ROUGH_POTION.get());
        stack.set(ModComponents.SOURCE_POTION.get(), sourceId);
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.empty(), Optional.empty(), effects));
        return stack;
    }

    private static MobEffectInstance dilute(MobEffectInstance source) {
        int duration = source.getDuration();
        if (duration >= 0) {
            double scaled = duration * Config.getDurationMultiplier();
            duration = Config.roundDurationUp() ? (int) Math.ceil(scaled) : (int) Math.floor(scaled);
            duration = Math.max(Config.getMinDurationTicks(), duration);
        }
        int amplifier = Math.max(Config.getMinAmplifier(),
                source.getAmplifier() - Config.getAmplifierReduction());
        return new MobEffectInstance(source.getEffect(), duration, amplifier,
                source.isAmbient(), source.isVisible(), source.showIcon());
    }

    @Nullable
    public static Holder<Potion> sourcePotion(ItemStack stack) {
        ResourceLocation id = stack.get(ModComponents.SOURCE_POTION.get());
        return id == null ? null : BuiltInRegistries.POTION.getHolder(id).orElse(null);
    }

    @Nullable
    public static ResourceLocation sourceId(ItemStack stack) {
        return stack.get(ModComponents.SOURCE_POTION.get());
    }

    @Nullable
    public static ResourceLocation idOf(Holder<Potion> potion) {
        return potion.unwrapKey().map(key -> key.location()).orElse(null);
    }

    public static ResourceLocation recipeId(ResourceLocation sourceId) {
        String path = "rough_" + sourceId.getNamespace() + "_" + sourceId.getPath() + "_from_cauldron";
        return ResourceLocation.fromNamespaceAndPath(BareBonesBrews.MODID, path);
    }
}
