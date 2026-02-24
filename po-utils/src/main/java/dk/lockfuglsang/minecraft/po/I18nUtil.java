package dk.lockfuglsang.minecraft.po;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
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
    private static volatile List<String> supportedLocaleKeys;
    private static final String DEFAULT_LOCALE_KEY = "en";
    private static final String I18N_ZIP_RESOURCE = "i18n.zip";
    private static final String SUPPORTED_LOCALES_FILE = "supported-locales.txt";
    private static final String TRANSLATION_SUPPORT_URL = "https://crowdin.com/project/uskyblock-revived";
    private static final TagResolver SEMANTIC_STYLE_TAGS = TagResolver.resolver(
        Placeholder.styling("muted", NamedTextColor.GRAY),
        Placeholder.styling("primary", NamedTextColor.AQUA),
        Placeholder.styling("secondary", NamedTextColor.GREEN),
        Placeholder.styling("cmd", NamedTextColor.AQUA),
        Placeholder.styling("success", NamedTextColor.GREEN),
        Placeholder.styling("error", NamedTextColor.RED)
    );

    /**
     * Translates the given {@link String} to the configured language and resolves MiniMessage placeholders using the
     * provided {@link TagResolver}s. Returns the given string if no translation is available. Returns an empty
     * component if the given key is null or empty.
     *
     * @param text      String to translate.
     * @param resolvers MiniMessage tag resolvers.
     * @return Translated Component.
     */
    public static @NotNull Component tr(@Nullable String text, @NotNull TagResolver... resolvers) {
        return tr(text, null, resolvers);
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
    public static @NotNull Component tr(@Nullable String text, @Nullable Style style, @NotNull TagResolver... resolvers) {
        return getI18n().tr(text, style, resolvers);
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
     * @deprecated Use {@link #tr(String, TagResolver...)} instead.
     */
    @Deprecated
    @NotNull
    public static String trLegacy(@Nullable String text, @Nullable Object... args) {
        return legacy(getI18n().tr(text, args));
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
    public static String trLegacy(@Nullable String text, @NotNull TagResolver... resolvers) {
        return legacy(tr(text, resolvers));
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
    public static String trLegacy(@Nullable String text, @Nullable Style style, @NotNull TagResolver... resolvers) {
        return legacy(tr(text, style, resolvers));
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
     * Returns the URL where users can help improve translations.
     *
     * @return Translation contribution URL.
     */
    public static @NotNull String getTranslationSupportUrl() {
        return TRANSLATION_SUPPORT_URL;
    }

    /**
     * Returns all supported locale keys discovered from packaged translations and plugin overrides.
     *
     * @return Sorted list of locale keys.
     */
    public static @NotNull List<String> getSupportedLocaleKeys() {
        List<String> result = supportedLocaleKeys;
        if (result == null) {
            synchronized (LOCK) {
                result = supportedLocaleKeys;
                if (result == null) {
                    result = discoverSupportedLocaleKeys();
                    supportedLocaleKeys = result;
                }
            }
        }
        return result;
    }

    /**
     * Finds an exact supported locale key match.
     * Matching is case-insensitive and treats '-' and '_' as equivalent separators.
     *
     * @param localeKey Locale key to match.
     * @return Exact supported locale key if present.
     */
    public static @NotNull Optional<String> findSupportedLocaleKey(@Nullable String localeKey) {
        if (localeKey == null || localeKey.trim().isEmpty()) {
            return Optional.empty();
        }
        String canonical = canonicalLocaleKey(localeKey);
        for (String supported : getSupportedLocaleKeys()) {
            if (canonicalLocaleKey(supported).equals(canonical)) {
                return Optional.of(supported);
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves a locale key to the best supported locale.
     * Matching order is: exact key, exact locale, language-only, then first supported variant of the same language.
     *
     * @param localeKey Locale key to resolve.
     * @return Best supported locale key, if any.
     */
    public static @NotNull Optional<String> resolveSupportedLocaleKey(@Nullable String localeKey) {
        Optional<String> exact = findSupportedLocaleKey(localeKey);
        if (exact.isPresent()) {
            return exact;
        }
        Locale parsed = getLocale(localeKey);
        if (parsed == null) {
            return Optional.empty();
        }
        return resolveSupportedLocaleKey(parsed);
    }

    /**
     * Resolves a locale to the best supported locale.
     * Matching order is: exact locale, language-only, then first supported variant of the same language.
     *
     * @param localeToResolve Locale to resolve.
     * @return Best supported locale key, if any.
     */
    public static @NotNull Optional<String> resolveSupportedLocaleKey(@Nullable Locale localeToResolve) {
        if (localeToResolve == null) {
            return Optional.empty();
        }
        String language = localeToResolve.getLanguage();
        String country = localeToResolve.getCountry();
        String variant = localeToResolve.getVariant();
        if (language.isEmpty()) {
            return Optional.empty();
        }

        if (!country.isEmpty() && !variant.isEmpty()) {
            Optional<String> full = findSupportedLocaleKey(language + "_" + country + "_" + variant);
            if (full.isPresent()) {
                return full;
            }
        }
        if (!country.isEmpty()) {
            Optional<String> langCountry = findSupportedLocaleKey(language + "_" + country);
            if (langCountry.isPresent()) {
                return langCountry;
            }
        } else if (!variant.isEmpty()) {
            Optional<String> langVariant = findSupportedLocaleKey(language + "__" + variant);
            if (langVariant.isPresent()) {
                return langVariant;
            }
        }

        Optional<String> languageOnly = findSupportedLocaleKey(language);
        if (languageOnly.isPresent()) {
            return languageOnly;
        }

        String languagePrefix = canonicalLocaleKey(language + "_");
        for (String supported : getSupportedLocaleKeys()) {
            if (canonicalLocaleKey(supported).startsWith(languagePrefix)) {
                return Optional.of(supported);
            }
        }
        return Optional.empty();
    }

    private static @NotNull List<String> discoverSupportedLocaleKeys() {
        Set<String> locales = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        addSupportedLocaleKeysFromPluginFolder(locales);
        addSupportedLocaleKeysFromZipInJar(locales);
        locales.add(DEFAULT_LOCALE_KEY);
        return List.copyOf(locales);
    }

    private static void addSupportedLocaleKeysFromZipInJar(@NotNull Set<String> locales) {
        try (
            InputStream in = I18nUtil.class.getClassLoader().getResourceAsStream(I18N_ZIP_RESOURCE);
            ZipInputStream zin = in != null ? new ZipInputStream(in, StandardCharsets.UTF_8) : null
        ) {
            if (zin == null) {
                return;
            }
            ZipEntry nextEntry;
            while ((nextEntry = zin.getNextEntry()) != null) {
                String entryName = nextEntry.getName();
                if (entryName.equalsIgnoreCase(SUPPORTED_LOCALES_FILE)) {
                    addSupportedLocaleKeysFromStream(locales, zin);
                } else if (entryName.toLowerCase(Locale.ROOT).endsWith(".po")) {
                    addSupportedLocaleKeyFromPath(locales, entryName);
                }
            }
        } catch (IOException e) {
            log.info("Unable to load supported locales from " + I18N_ZIP_RESOURCE + ": " + e);
        }
    }

    private static void addSupportedLocaleKeysFromPluginFolder(@NotNull Set<String> locales) {
        if (dataFolder == null) {
            return;
        }
        File i18nFolder = new File(dataFolder, "i18n");
        if (!i18nFolder.exists() || !i18nFolder.isDirectory()) {
            return;
        }

        File supportedFile = new File(i18nFolder, SUPPORTED_LOCALES_FILE);
        if (supportedFile.exists() && supportedFile.isFile()) {
            try (InputStream in = new FileInputStream(supportedFile)) {
                addSupportedLocaleKeysFromStream(locales, in);
            } catch (IOException e) {
                log.info("Unable to read supported locales from " + supportedFile + ": " + e);
            }
        }

        File[] poFiles = i18nFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".po"));
        if (poFiles != null) {
            for (File poFile : poFiles) {
                String name = poFile.getName();
                int suffixIndex = name.toLowerCase(Locale.ROOT).lastIndexOf(".po");
                if (suffixIndex > 0) {
                    locales.add(name.substring(0, suffixIndex));
                }
            }
        }
    }

    private static void addSupportedLocaleKeysFromStream(@NotNull Set<String> locales, @NotNull InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                locales.add(trimmed);
            }
        }
    }

    private static void addSupportedLocaleKeyFromPath(@NotNull Set<String> locales, @NotNull String path) {
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        int suffixIndex = fileName.toLowerCase(Locale.ROOT).lastIndexOf(".po");
        if (suffixIndex > 0) {
            locales.add(fileName.substring(0, suffixIndex));
        }
    }

    private static @NotNull String canonicalLocaleKey(@NotNull String localeKey) {
        return localeKey.trim().replace('-', '_').toLowerCase(Locale.ROOT);
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
     * @param text String to mark.
     * @return Input String.
     */
    @Contract(value = "null -> null", pure = true)
    public static String marktr(@Nullable String text) {
        return text;
    }

    /**
     * Formats the given MiniMessage string without translating and resolves named placeholders.
     * Returns an empty String if the given String is null or empty.
     *
     * @param text      String as Adventure MiniMessage.
     * @param resolvers MiniMessage resolvers.
     * @return Formatted legacy String with color codes.
     */
    @NotNull
    public static String miniToLegacy(@Nullable String text, @NotNull TagResolver... resolvers) {
        if (text != null && !text.isEmpty()) {
            return legacy(deserializeMiniMessage(text, resolvers));
        }
        return "";
    }

    /**
     * Formats the given MiniMessage string without translating and resolves named placeholders.
     *
     * @param text      String as Adventure MiniMessage.
     * @param resolvers MiniMessage resolvers.
     * @return Formatted text as a component.
     */
    @NotNull
    public static Component parseMini(@Nullable String text, @NotNull TagResolver... resolvers) {
        if (text != null && !text.isEmpty()) {
            return deserializeMiniMessage(text, resolvers);
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
     * @param locale The desired Locale. If null, Locale.ENGLISH is used.
     */
    public static void initialize(@NotNull File folder, @Nullable Locale locale) {
        synchronized (LOCK) {
            dataFolder = folder;
            I18nUtil.locale = locale;
            i18n = new I18n(getLocale());
            supportedLocaleKeys = null;
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
     * @param locale Locale to set.
     */
    public static void setLocale(@Nullable Locale locale) {
        synchronized (LOCK) {
            I18nUtil.locale = locale;
            clearCache();
        }
    }

    /**
     * Clears the I18n cache, forcing a reload of the .po files the next time that {@link I18nUtil#getLocale()} is accessed.
     */
    public static void clearCache() {
        synchronized (LOCK) {
            i18n = new I18n(getLocale());
            supportedLocaleKeys = null;
        }
    }

    /**
     * Converts the given {@link String} to a {@link Locale}.
     *
     * @param language Language code..
     * @return Locale based on the given string.
     */
    @Contract(value = "null -> null", pure = true)
    public static Locale getLocale(@Nullable String language) {
        if (language != null) {
            String[] parts = language.split("[-_]");
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
        return LegacyComponentSerializer.legacySection();
    }

    private static @NotNull MiniMessage getMiniMessage() {
        return MiniMessage.miniMessage();
    }

    @Deprecated
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

    private static @NotNull Component deserializeMiniMessage(@NotNull String message, @NotNull TagResolver... resolvers) {
        try {
            return getMiniMessage().deserialize(message, resolveSemanticTags(resolvers));
        } catch (RuntimeException e) {
            // No legacy conversion/parsing path: preserve input as plain text if MiniMessage parsing fails.
            return Component.text(message);
        }
    }

    private static @NotNull TagResolver resolveSemanticTags(@NotNull TagResolver... resolvers) {
        if (resolvers == null || resolvers.length == 0) {
            return SEMANTIC_STYLE_TAGS;
        }
        TagResolver[] combined = new TagResolver[resolvers.length + 1];
        combined[0] = SEMANTIC_STYLE_TAGS;
        System.arraycopy(resolvers, 0, combined, 1, resolvers.length);
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
                InputStream in = getClass().getClassLoader().getResourceAsStream(I18N_ZIP_RESOURCE);
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
                log.info("Unable to load translations from " + I18N_ZIP_RESOURCE + "!" + localeKey + ".po: " + e);
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

        /**
         * @deprecated Use {@link #tr(String, TagResolver...)} instead.
         */
        @Deprecated
        private @NotNull Component tr(@Nullable String text, @Nullable Object... args) {
            if (text == null || text.trim().isEmpty()) {
                return Component.empty();
            }
            String translated = translations.getProperty(text);
            if (translated != null && !translated.trim().isEmpty()) {
                return format(translated, args);
            }
            return format(text, args);
        }

        public @NotNull Component tr(@Nullable String text, @Nullable Style style, @NotNull TagResolver... resolvers) {
            if (text == null || text.trim().isEmpty()) {
                return Component.empty();
            }
            String translated = translations.getProperty(text);
            Component result;
            if (translated != null && !translated.trim().isEmpty()) {
                result = deserializeMiniMessage(translated, resolvers);
            } else {
                result = deserializeMiniMessage(text, resolvers);
            }
            if (style != null) {
                result = result.applyFallbackStyle(style);
            }
            return result;
        }

        @Deprecated
        private @NotNull Component format(@NotNull String text, @Nullable Object[] args) {
            try {
                Object[] normalizedArgs = normalizeArgsForMiniMessage(args);
                String formatted = new MessageFormat(text, getLocale()).format(normalizedArgs);
                return deserializeMiniMessage(formatted);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Problem with: '" + text + "'", e);
            }
        }

        public Locale getLocale() {
            return locale;
        }
    }
}
