package lasea.barebonesbrews;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lasea.barebonesbrews.brewing.HeatSourceRule;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = BareBonesBrews.MODID)
public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_ROUGH_POTIONS = BUILDER
            .comment("Create rough (diluted) variants of registered potions.")
            .define("enable_rough_potions", true);

    public static final ModConfigSpec.DoubleValue DURATION_MULTIPLIER = BUILDER
            .comment("Rough potions last this fraction of the original duration. 0.5 = half.")
            .defineInRange("duration_multiplier", 0.5D, 0.01D, 1.0D);

    public static final ModConfigSpec.BooleanValue ROUND_DURATION_UP = BUILDER
            .comment("Round the diluted duration up instead of down, so short potions stay useful.")
            .define("round_duration_up", true);

    public static final ModConfigSpec.IntValue MIN_DURATION_TICKS = BUILDER
            .comment("Never derive a duration shorter than this many ticks (20 ticks = 1 second).")
            .defineInRange("min_duration_ticks", 200, 1, 72000);

    public static final ModConfigSpec.IntValue AMPLIFIER_REDUCTION = BUILDER
            .comment("Levels removed from every effect. 1 turns Strength II into Strength I.")
            .defineInRange("amplifier_reduction", 1, 0, 10);

    public static final ModConfigSpec.IntValue MIN_AMPLIFIER = BUILDER
            .comment("Amplifier floor; 0 is level I.")
            .defineInRange("min_amplifier", 0, 0, 10);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_NAMESPACES = BUILDER
            .comment("Potion namespaces to leave alone, e.g. \"minecraft\" to only convert modded potions.")
            .defineListAllowEmpty("excluded_namespaces", List.of(), () -> "", Config::isNamespace);

    public static final ModConfigSpec.BooleanValue ENABLE_CAULDRON = BUILDER
            .comment("Enable cauldron brewing and its generated recipes.")
            .define("enable_cauldron_recipes", true);

    public static final ModConfigSpec.BooleanValue GENERATE_CAULDRON_RECIPES = BUILDER
            .comment("Generate default cauldron recipes. Disable to use only datapack/KubeJS recipes; brewing remains enabled.")
            .define("generate_cauldron_recipes", true);

    private static final List<String> DEFAULT_HEAT_SOURCES = List.of("#barebonesbrews:heat_sources");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CAULDRON_HEAT_SOURCES = BUILDER
            .comment("Allowed blocks immediately below vanilla cauldrons. Replaces the whole list; [] disables heating.",
                    "Accepts block IDs, #block tags and state conditions, e.g. minecraft:furnace[lit=true].",
                    "Blocks with a lit property must be lit unless explicitly matched with [lit=false].",
                    "The default tag can be edited with datapacks or KubeJS ServerEvents.tags('block', ...).",
                    "Unknown IDs, missing properties and invalid property values never match. Does not affect Hexalia.")
            .defineListAllowEmpty("cauldron_heat_sources", DEFAULT_HEAT_SOURCES,
                    () -> "minecraft:magma_block", HeatSourceRule::isValid);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile Set<String> excluded = Set.of();

    private record HeatRules(List<? extends String> selectors, List<HeatSourceRule> rules) {}
    private static volatile HeatRules heatRules = new HeatRules(DEFAULT_HEAT_SOURCES,
            DEFAULT_HEAT_SOURCES.stream().map(HeatSourceRule::parse).toList());

    private static volatile boolean ready;

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            rebuildExclusions();
        }
    }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            rebuildExclusions();
        }
    }

    private static void rebuildExclusions() {
        Set<String> namespaces = new HashSet<>();
        try {
            for (String namespace : EXCLUDED_NAMESPACES.get()) {
                namespaces.add(namespace);
            }
        } catch (IllegalStateException ignored) {}
        excluded = Set.copyOf(namespaces);
        ready = true;
    }

    private static boolean isNamespace(Object value) {
        return value instanceof String string && string.matches("[a-z0-9_.-]+");
    }

    private static boolean readBoolean(ModConfigSpec.BooleanValue value, boolean fallback) {
        if (!ready) {
            return fallback;
        }
        try {
            return value.getAsBoolean();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    private static int readInt(ModConfigSpec.IntValue value, int fallback) {
        if (!ready) {
            return fallback;
        }
        try {
            return value.getAsInt();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    private static double readDouble(ModConfigSpec.DoubleValue value, double fallback) {
        if (!ready) {
            return fallback;
        }
        try {
            return value.getAsDouble();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    public static boolean isNamespaceExcluded(String namespace) {
        return excluded.contains(namespace);
    }

    public static boolean roughPotionsEnabled() {
        return readBoolean(ENABLE_ROUGH_POTIONS, true);
    }

    public static boolean cauldronRecipesEnabled() {
        return readBoolean(ENABLE_CAULDRON, true);
    }

    public static boolean generateCauldronRecipes() {
        return readBoolean(GENERATE_CAULDRON_RECIPES, true);
    }

    public static boolean isCauldronHeatSource(BlockState state) {
        List<? extends String> selectors = DEFAULT_HEAT_SOURCES;
        if (ready) {
            try {
                selectors = CAULDRON_HEAT_SOURCES.get();
            } catch (IllegalStateException ignored) {}
        }
        HeatRules snapshot = heatRules;
        if (!snapshot.selectors().equals(selectors)) {
            snapshot = new HeatRules(List.copyOf(selectors), selectors.stream().map(HeatSourceRule::parse).toList());
            heatRules = snapshot;
        }
        return snapshot.rules().stream().anyMatch(rule -> rule.matches(state));
    }

    public static double getDurationMultiplier() {
        return readDouble(DURATION_MULTIPLIER, 0.5D);
    }

    public static int getAmplifierReduction() {
        return readInt(AMPLIFIER_REDUCTION, 1);
    }

    public static int getMinAmplifier() {
        return readInt(MIN_AMPLIFIER, 0);
    }

    public static int getMinDurationTicks() {
        return readInt(MIN_DURATION_TICKS, 200);
    }

    public static boolean roundDurationUp() {
        return readBoolean(ROUND_DURATION_UP, true);
    }

    private Config() {}
}
