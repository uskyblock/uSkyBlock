package us.talabrek.ultimateskyblock.message;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class Placeholder {
    private static final Pattern LOCALE_TAG_PATTERN = Pattern.compile("^[A-Za-z]{2,8}(?:[-_][A-Za-z0-9]{2,8})*$");

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

    public static @NotNull TagResolver number(@TagPattern final @NotNull String key, final @NotNull Number value) {
        requireNonNull(value, "value");
        return TagResolver.resolver(key, (argumentQueue, context) -> {
            Locale defaultLocale = getActiveLocale();
            NumberFormat numberFormat;
            if (argumentQueue.hasNext()) {
                String first = argumentQueue.pop().value();
                if (argumentQueue.hasNext()) {
                    numberFormat = getNumberFormat(defaultLocale, first, argumentQueue.pop().value());
                } else {
                    numberFormat = getNumberFormat(defaultLocale, first, null);
                }
            } else {
                numberFormat = DecimalFormat.getInstance(defaultLocale);
            }
            return Tag.inserting(context.deserialize(numberFormat.format(value)));
        });
    }

    public static @NotNull TagResolver number(
        @TagPattern final @NotNull String key,
        final @NotNull Number value,
        final @NotNull Style style
    ) {
        requireNonNull(value, "value");
        requireNonNull(style, "style");
        return TagResolver.resolver(key, (argumentQueue, context) -> {
            Locale defaultLocale = getActiveLocale();
            NumberFormat numberFormat;
            if (argumentQueue.hasNext()) {
                String first = argumentQueue.pop().value();
                if (argumentQueue.hasNext()) {
                    numberFormat = getNumberFormat(defaultLocale, first, argumentQueue.pop().value());
                } else {
                    numberFormat = getNumberFormat(defaultLocale, first, null);
                }
            } else {
                numberFormat = DecimalFormat.getInstance(defaultLocale);
            }
            Component component = context.deserialize(numberFormat.format(value)).applyFallbackStyle(style);
            return Tag.inserting(component);
        });
    }

    private static @NotNull NumberFormat getNumberFormat(@NotNull Locale defaultLocale, @NotNull String first, String second) {
        if (second != null) {
            Locale locale = parseLocaleTagOrDefault(first, defaultLocale);
            return new DecimalFormat(second, DecimalFormatSymbols.getInstance(locale));
        } else if (isLocaleTag(first)) {
            Locale locale = parseLocaleTagOrDefault(first, defaultLocale);
            return DecimalFormat.getInstance(locale);
        } else {
            return new DecimalFormat(first, DecimalFormatSymbols.getInstance(defaultLocale));
        }
    }

    private static boolean isLocaleTag(@NotNull String value) {
        if (!LOCALE_TAG_PATTERN.matcher(value).matches()) {
            return false;
        }
        Locale locale = Locale.forLanguageTag(value.replace('_', '-'));
        return !locale.getLanguage().isEmpty();
    }

    private static @NotNull Locale parseLocaleTagOrDefault(@NotNull String value, @NotNull Locale fallback) {
        Locale locale = Locale.forLanguageTag(value.replace('_', '-'));
        return locale.getLanguage().isEmpty() ? fallback : locale;
    }

    private static @NotNull Locale getActiveLocale() {
        try {
            return I18nUtil.getI18n().getLocale();
        } catch (IllegalStateException ignored) {
            return I18nUtil.getLocale();
        }
    }
}
