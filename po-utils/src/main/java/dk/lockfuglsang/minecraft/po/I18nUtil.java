package dk.lockfuglsang.minecraft.po;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Convenience util for supporting static imports.
 */
public enum I18nUtil {
    ;
    private static final Logger log = Logger.getLogger(I18nUtil.class.getName());

    // Dedicated lock for synchronizing modifications
    private static final Object LOCK = new Object();

    private static volatile I18n i18n;
    private static volatile Locale locale;
    private static volatile File dataFolder;
    private static volatile LegacyComponentSerializer legacySerializer;
    private static volatile MiniMessage miniMessage;
    private static final TagResolver SEMANTIC_STYLE_TAGS = TagResolver.resolver(
        Placeholder.styling("muted", NamedTextColor.GRAY),
        Placeholder.styling("primary", NamedTextColor.AQUA),
        Placeholder.styling("secondary", NamedTextColor.GREEN),
        Placeholder.styling("cmd", NamedTextColor.AQUA),
        Placeholder.styling("success", NamedTextColor.GREEN),
        Placeholder.styling("error", NamedTextColor.RED)
    );

    /**
     * Translates the given {@link String} to the configured language. Returns the given string if no translation is
     * available. Returns an empty component if the given key is null or empty.
     *
     * @param s String to translate.
     * @return Translated Component.
     */
    public static @NotNull Component tr(@Nullable String s) {
        return getI18n().tr(s);
    }

    /**
     * Translates the given {@link String} to the configured language. Formats with the given {@link Object}. Returns
     * the given String if no translation is available. Returns an empty component if the given key is null or empty.
     *
     * @param text String to translate.
     * @param args Arguments to format.
     * @return Translated Component.
     */
    public static @NotNull Component tr(@Nullable String text, @Nullable Object... args) {
        return getI18n().tr(text, args);
    }

    /**
     * Translates the given {@link String} to the configured language and resolves MiniMessage placeholders using the
     * provided {@link TagResolver}s. Returns the given string if no translation is available. Returns an empty
     * component if the given key is null or empty.
     *
     * @param text      String to translate.
     * @param resolvers MiniMessage tag resolvers.
     * @return Translated Component.
     */
    public static @NotNull Component tr(@Nullable String text, @Nullable TagResolver... resolvers) {
        return getI18n().tr(text, resolvers);
    }

    /**
     * Translates the given {@link String} and serializes to legacy-format text using § color/style codes.
     *
     * @param text String to translate.
     * @return Translated legacy-formatted String.
     */
    @NotNull
    public static String trLegacy(@Nullable String text) {
        return legacy(tr(text));
    }

    /**
     * Translates and formats the given {@link String}, then serializes to legacy-format text using § color/style codes.
     *
     * @param text String to translate.
     * @param args Arguments to format.
     * @return Translated legacy-formatted String.
     */
    @NotNull
    public static String trLegacy(@Nullable String text, @Nullable Object... args) {
        return legacy(tr(text, args));
    }

    /**
     * Translates the given {@link String}, resolves MiniMessage placeholders using {@link TagResolver}s, and
     * serializes to legacy-format text using § color/style codes.
     *
     * @param text      String to translate.
     * @param resolvers MiniMessage tag resolvers.
     * @return Translated legacy-formatted String.
     */
    @NotNull
    public static String trLegacy(@Nullable String text, @Nullable TagResolver... resolvers) {
        return legacy(tr(text, resolvers));
    }

    /**
     * Creates a named MiniMessage placeholder resolver from a legacy formatted string.
     * <p>
     * This is intended for edge-cases where dynamic values still originate from legacy APIs that provide {@code §}
     * formatting.
     *
     * @param name        Placeholder name (without angle brackets).
     * @param legacyValue Value that may contain legacy formatting.
     * @return Tag resolver that inserts the deserialized component.
     */
    @NotNull
    public static TagResolver legacyArg(@TagPattern @NotNull String name, @Nullable String legacyValue) {
        Component value = fromLegacy(legacyValue);
        return Placeholder.component(name, value);
    }


    @NotNull
    public static Component fromLegacy(@Nullable String legacy) {
        return legacy != null ? getLegacySerializer().deserialize(legacy) : Component.empty();
    }

    /**
     * Converts a {@link Component} to legacy-formatted text using § color/style codes.
     *
     * @param component component to serialize.
     * @return legacy-formatted string.
     */
    @NotNull
    public static String legacy(@Nullable Component component) {
        if (component == null) {
            return "";
        }
        return getLegacySerializer().serialize(component);
    }

    /**
     * Marks the given {@link String} for translation for the .po files.
     *
     * @param key String to mark.
     * @return Input String.
     */
    @Contract(value = "null -> null", pure = true)
    public static String marktr(@Nullable String key) {
        return key;
    }

    /**
     * Formats the given MiniMessage string without translating and resolves named placeholders.
     * Returns an empty String if the given String is null or empty.
     *
     * @param message   String as Adventure MiniMessage.
     * @param resolvers MiniMessage resolvers.
     * @return Formatted legacy String with color codes.
     */
    @NotNull
    public static String miniToLegacy(@Nullable String message, @Nullable TagResolver... resolvers) {
        if (message != null && !message.isEmpty()) {
            return legacy(deserializeMessage(message, resolvers));
        }
        return "";
    }

    /**
     * Formats the given MiniMessage string without translating and resolves named placeholders.
     *
     * @param message   String as Adventure MiniMessage.
     * @param resolvers MiniMessage resolvers.
     * @return Formatted message as a component.
     */
    @NotNull
    public static Component parseMini(@Nullable String message, @Nullable TagResolver... resolvers) {
        if (message != null && !message.isEmpty()) {
            return deserializeMessage(message, resolvers);
        }
        return Component.empty();
    }

    /**
     * Gets the {@link I18n} instance representing the configured {@link Locale}. Lazy-loads if necessary.
     *
     * @return I18n instance for the configured locale.
     */
    public static I18n getI18n() {
        I18n result = i18n;
        if (result == null) {
            throw new IllegalStateException("I18nUtil not initialized!");
        }
        return result;
    }

    /**
     * Initializes the I18nUtil. This method is called whenever the plugin loads or reloads.
     * Thread safety is ensured by synchronizing modifications of shared fields.
     *
     * @param folder The plugin's data folder.
     * @param loc    The desired Locale. If null, Locale.ENGLISH is used.
     */
    public static void initialize(@NotNull File folder, @Nullable Locale loc) {
        synchronized (LOCK) {
            dataFolder = folder;
            locale = loc;
            i18n = new I18n(getLocale());
        }
    }

    /**
     * Returns the configured {@link Locale} or the default (Locale.ENGLISH) if unset.
     *
     * @return Configured Locale.
     */
    @NotNull
    public static Locale getLocale() {
        return locale != null ? locale : Locale.ENGLISH;
    }

    /**
     * Sets the {@link Locale}. Resets to the default locale if NULL is given.
     * The I18n cache is cleared so that translations are reloaded.
     *
     * @param loc Locale to set.
     */
    public static void setLocale(@Nullable Locale loc) {
        synchronized (LOCK) {
            locale = loc;
            clearCache();
        }
    }

    /**
     * Clears the I18n cache, forcing a reload of the .po files the next time that {@link I18nUtil#getLocale()} is accessed.
     */
    public static void clearCache() {
        synchronized (LOCK) {
            i18n = new I18n(getLocale());
        }
    }

    /**
     * Converts the given {@link String} to a {@link Locale}.
     *
     * @param lang Language code..
     * @return Locale based on the given string.
     */
    @Contract(value = "null -> null", pure = true)
    public static Locale getLocale(@Nullable String lang) {
        if (lang != null) {
            String[] parts = lang.split("[-_]");
            if (parts.length >= 3) {
                return Locale.of(parts[0], parts[1], parts[2]);
            } else if (parts.length == 2) {
                return Locale.of(parts[0], parts[1]);
            } else {
                return Locale.of(parts[0]);
            }
        }
        return null;
    }

    private static @NotNull LegacyComponentSerializer getLegacySerializer() {
        LegacyComponentSerializer serializer = legacySerializer;
        if (serializer == null) {
            serializer = LegacyComponentSerializer.builder().character('\u00a7').build();
            legacySerializer = serializer;
        }
        return serializer;
    }

    private static @NotNull MiniMessage getMiniMessage() {
        MiniMessage serializer = miniMessage;
        if (serializer == null) {
            serializer = MiniMessage.miniMessage();
            miniMessage = serializer;
        }
        return serializer;
    }

    private static @Nullable Object[] normalizeArgsForMiniMessage(@Nullable Object[] args) {
        if (args == null) {
            return null;
        }
        Object[] normalized = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Component component) {
                normalized[i] = getMiniMessage().serialize(component);
            } else {
                normalized[i] = arg;
            }
        }
        return normalized;
    }

    private static @Nullable Object[] normalizeArgsForLegacy(@Nullable Object[] args) {
        if (args == null) {
            return null;
        }
        Object[] normalized = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Component component) {
                normalized[i] = legacy(component);
            } else {
                normalized[i] = arg;
            }
        }
        return normalized;
    }

    private static @NotNull Component deserializeMessage(@NotNull String message) {
        return deserializeMessage(message, (TagResolver[]) null);
    }

    private static @NotNull Component deserializeMessage(@NotNull String message, @Nullable TagResolver... resolvers) {
        try {
            return getMiniMessage().deserialize(message, resolveSemanticTags(resolvers));
        } catch (RuntimeException e) {
            // No legacy conversion/parsing path: preserve input as plain text if MiniMessage parsing fails.
            return Component.text(message);
        }
    }

    private static @NotNull TagResolver resolveSemanticTags(@Nullable TagResolver... resolvers) {
        if (resolvers == null || resolvers.length == 0) {
            return SEMANTIC_STYLE_TAGS;
        }
        TagResolver[] nonNullResolvers = Arrays.stream(resolvers)
            .filter(Objects::nonNull)
            .toArray(TagResolver[]::new);
        if (nonNullResolvers.length == 0) {
            return SEMANTIC_STYLE_TAGS;
        }
        TagResolver[] combined = new TagResolver[nonNullResolvers.length + 1];
        combined[0] = SEMANTIC_STYLE_TAGS;
        System.arraycopy(nonNullResolvers, 0, combined, 1, nonNullResolvers.length);
        return TagResolver.resolver(combined);
    }

    /**
     * Proxy between uSkyBlock and org.xnap.commons.i18n.I18n
     */
    public static class I18n {
        private final Locale locale;
        private final Properties translations = new Properties();

        I18n(Locale locale) {
            this.locale = locale;
            List<String> localeCandidates = getLocaleCandidates();

            // Order of these calls is important here, because it specifies the priority of the files (git rlf/1233).
            for (String localeKey : localeCandidates) {
                addPropertiesFromZipInJar(localeKey).ifPresent(properties -> {
                    translations.putAll(properties);
                    log.log(Level.INFO, "Added {0} translations from ZIP inside JAR ({1}).",
                        new Object[]{properties.size(), localeKey});
                });
            }
            for (String localeKey : localeCandidates) {
                addPropertiesFromJar(localeKey).ifPresent(properties -> {
                    translations.putAll(properties);
                    log.log(Level.INFO, "Added {0} translations from the JAR ({1}).",
                        new Object[]{properties.size(), localeKey});
                });
            }
            for (String localeKey : localeCandidates) {
                addPropertiesFromPluginFolder(localeKey).ifPresent(properties -> {
                    translations.putAll(properties);
                    log.log(Level.INFO, "Added {0} translations from the plugin directory ({1}).",
                        new Object[]{properties.size(), localeKey});
                });
            }

            log.log(Level.INFO, "Loaded {0} translations.", translations.size());
        }

        private @NotNull List<String> getLocaleCandidates() {
            List<String> candidates = new ArrayList<>(3);
            String language = locale.getLanguage();
            String country = locale.getCountry();
            String variant = locale.getVariant();

            if (!language.isEmpty()) {
                candidates.add(language);
                if (!country.isEmpty()) {
                    candidates.add(language + "_" + country);
                    if (!variant.isEmpty()) {
                        candidates.add(language + "_" + country + "_" + variant);
                    }
                } else if (!variant.isEmpty()) {
                    candidates.add(language + "__" + variant);
                }
            }

            String fullLocale = locale.toString();
            if (!fullLocale.isEmpty() && !candidates.contains(fullLocale)) {
                candidates.add(fullLocale);
            }
            return candidates;
        }

        private Optional<Properties> addPropertiesFromJar(@NotNull String localeKey) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("po/" + localeKey + ".po")) {
                if (in == null) {
                    return Optional.empty();
                }
                Properties properties = POParser.asProperties(in);
                return Optional.ofNullable(properties);
            } catch (IOException e) {
                log.info("Unable to read translations from po/" + localeKey + ".po: " + e);
            }
            return Optional.empty();
        }

        private Optional<Properties> addPropertiesFromZipInJar(@NotNull String localeKey) {
            // We zip the .po files, since they are currently half the footprint of the jar.
            try (
                InputStream in = getClass().getClassLoader().getResourceAsStream("i18n.zip");
                ZipInputStream zin = in != null ? new ZipInputStream(in, StandardCharsets.UTF_8) : null
            ) {
                ZipEntry nextEntry;
                do {
                    nextEntry = zin != null ? zin.getNextEntry() : null;
                    if (nextEntry != null && nextEntry.getName().equalsIgnoreCase(localeKey + ".po")) {
                        Properties properties = POParser.asProperties(zin);
                        return Optional.ofNullable(properties);
                    }
                } while (nextEntry != null);
            } catch (IOException e) {
                log.info("Unable to load translations from i18n.zip!" + localeKey + ".po: " + e);
            }
            return Optional.empty();
        }

        private Optional<Properties> addPropertiesFromPluginFolder(@NotNull String localeKey) {
            File poFile = new File(dataFolder, "i18n" + File.separator + localeKey + ".po");
            if (poFile.exists()) {
                try (InputStream in = new FileInputStream(poFile)) {
                    Properties properties = POParser.asProperties(in);
                    return Optional.ofNullable(properties);
                } catch (IOException e) {
                    log.info("Unable to load translations from i18n" + File.separator + localeKey + ".po: " + e);
                }
            }
            return Optional.empty();
        }

        public @NotNull Component tr(@Nullable String key, @Nullable Object... args) {
            if (key == null || key.trim().isEmpty()) {
                return Component.empty();
            }
            String propKey = translations.getProperty(key);
            if (propKey != null && !propKey.trim().isEmpty()) {
                return format(propKey, args);
            }
            return format(key, args);
        }

        public @NotNull Component tr(@Nullable String key, @Nullable TagResolver... resolvers) {
            if (key == null || key.trim().isEmpty()) {
                return Component.empty();
            }
            String propKey = translations.getProperty(key);
            if (propKey != null && !propKey.trim().isEmpty()) {
                return format(propKey, resolvers);
            }
            return format(key, resolvers);
        }

        private @NotNull Component format(@NotNull String propKey, @Nullable Object[] args) {
            try {
                Object[] normalizedArgs = normalizeArgsForMiniMessage(args);
                String formatted = new MessageFormat(propKey, getLocale()).format(normalizedArgs);
                return deserializeMessage(formatted);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Problem with: '" + propKey + "'", e);
            }
        }

        private @NotNull Component format(@NotNull String propKey, @Nullable TagResolver[] resolvers) {
            try {
                return deserializeMessage(propKey, resolvers);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Problem with: '" + propKey + "'", e);
            }
        }

        public Locale getLocale() {
            return locale;
        }
    }
}
