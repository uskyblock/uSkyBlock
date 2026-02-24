package us.talabrek.ultimateskyblock.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

public class Placeholder {

    private Placeholder() {
        // Static utility class
    }

    public static TagResolver.@NotNull Single unparsed(@TagPattern final @NotNull String key, final @NotNull String value) {
        requireNonNull(value, "value");
        return net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(key, Component.text(value));
    }

    public static TagResolver.@NotNull Single unparsed(@TagPattern final @NotNull String key, final @NotNull String value, @NotNull Style style) {
        requireNonNull(value, "value");
        requireNonNull(style, "style");
        return net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(key, Component.text(value, style));
    }

    public static TagResolver.@NotNull Single legacy(@TagPattern final @NotNull String key, final @NotNull String value) {
        requireNonNull(value, "value");
        Component component = LegacyComponentSerializer.legacySection().deserialize(value);
        return net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(key, component);
    }

    public static TagResolver.@NotNull Single legacy(@TagPattern final @NotNull String key, final @NotNull String value, @NotNull Style style) {
        requireNonNull(value, "value");
        requireNonNull(style, "style");
        Component component = LegacyComponentSerializer.legacySection().deserialize(value);
        component = component.applyFallbackStyle(style);
        return net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(key, component);
    }
}
