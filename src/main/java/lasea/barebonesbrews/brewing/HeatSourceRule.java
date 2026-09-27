package lasea.barebonesbrews.brewing;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

public record HeatSourceRule(ResourceLocation id, boolean tag, Map<String, String> properties) {
    private static final Pattern SELECTOR = Pattern.compile(
            "(#?)([a-z0-9_.-]+:[a-z0-9/._-]+)(?:\\[([a-z0-9_]+=[a-z0-9_.-]+(?:,[a-z0-9_]+=[a-z0-9_.-]+)*)\\])?");

    public HeatSourceRule {
        properties = Map.copyOf(properties);
    }

    public static HeatSourceRule parse(String value) {
        var match = SELECTOR.matcher(value.trim());
        if (!match.matches()) {
            throw new IllegalArgumentException("Expected namespace:block or #namespace:tag with optional [property=value]: " + value);
        }
        Map<String, String> properties = new HashMap<>();
        if (match.group(3) != null) {
            for (String entry : match.group(3).split(",")) {
                String[] pair = entry.split("=", 2);
                if (properties.putIfAbsent(pair[0], pair[1]) != null) {
                    throw new IllegalArgumentException("Duplicate heat source property: " + pair[0]);
                }
            }
        }
        return new HeatSourceRule(ResourceLocation.parse(match.group(2)), !match.group(1).isEmpty(), properties);
    }

    public static boolean isValid(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        try {
            parse(text);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean matches(BlockState state) {
        if (tag ? !state.is(TagKey.create(Registries.BLOCK, id))
                : !id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
            return false;
        }
        for (var entry : properties.entrySet()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property == null || !matchesProperty(state, property, entry.getValue())) {
                return false;
            }
        }
        return properties.containsKey("lit") || !state.hasProperty(BlockStateProperties.LIT)
                || state.getValue(BlockStateProperties.LIT);
    }

    private static <T extends Comparable<T>> boolean matchesProperty(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(expected -> expected.equals(state.getValue(property))).orElse(false);
    }
}
