package lasea.barebonesbrews;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Everything about the mod that a pack author might reasonably want to change without a recompile.
 *
 * <p>Rough stacks and recipes are created after configuration loads. No option is consulted while a
 * registry is mutable, so values are deterministic and never depend on event ordering.
 */
@EventBusSubscriber(modid = BareBonesBrews.MODID)
public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ------------------------------------------------------------------ general

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

    // ------------------------------------------------------------------ cauldron

    public static final ModConfigSpec.BooleanValue ENABLE_CAULDRON = BUILDER
            .comment("Generate the rough cauldron recipes (mushroom + the potion's original reagent).")
            .define("enable_cauldron_recipes", true);

    // ------------------------------------------------------------------ localization

    // The translated rough prefix wraps the source potion's normal translated name.

    public static final ModConfigSpec.BooleanValue SHOW_SUMMARY_MESSAGE = BUILDER
            .comment("Tell joining players how many rough potions were derived.")
            .define("show_summary_message", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile Set<String> excluded = Set.of();

    /**
     * True once FML has handed us a loaded config.
     *
     * <p>Accessors still tolerate early calls from creative-tab construction in unusual launch paths.
     */
    private static volatile boolean ready;

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) {
        rebuildExclusions();
    }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) {
        rebuildExclusions();
    }

    private static void rebuildExclusions() {
        Set<String> namespaces = new HashSet<>();
        try {
            for (String namespace : EXCLUDED_NAMESPACES.get()) {
                namespaces.add(namespace);
            }
        } catch (IllegalStateException ignored) {
            // Still unloaded; leave the previous set in place.
        }
        excluded = Set.copyOf(namespaces);
        ready = true;
    }

    private static boolean isNamespace(Object value) {
        return value instanceof String string && string.matches("[a-z0-9_.-]+");
    }

    // ------------------------------------------------------------------ accessors

    /** Reads a spec value, falling back to the declared default while the config is still unloaded. */
    private static <T> T read(ModConfigSpec.ConfigValue<T> value, T fallback) {
        if (!ready) {
            return fallback;
        }
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    /** Same as {@link #read}, for the primitive-typed spec values. */
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

    /** Mirrors the {@code enable_rough_potions} default of {@code true}. */
    public static boolean roughPotionsEnabled() {
        return readBoolean(ENABLE_ROUGH_POTIONS, true);
    }

    /** Mirrors the {@code enable_cauldron_recipes} default of {@code true}. */
    public static boolean cauldronRecipesEnabled() {
        return readBoolean(ENABLE_CAULDRON, true);
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

    /** Mirrors the {@code show_summary_message} default of {@code true}. */
    public static boolean showSummaryMessage() {
        return readBoolean(SHOW_SUMMARY_MESSAGE, true);
    }

    private Config() {}
}
