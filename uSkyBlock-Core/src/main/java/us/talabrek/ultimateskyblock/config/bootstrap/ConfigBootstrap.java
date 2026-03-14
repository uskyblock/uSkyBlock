package us.talabrek.ultimateskyblock.config.bootstrap;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.PluginConfig;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.migration.PluginConfigMigrator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigBootstrap {
    public static final String DEFAULT_LANGUAGE = "en";

    private final Path pluginDataDir;
    private final Logger logger;
    private final String defaultLanguage;

    public ConfigBootstrap(@NotNull Path pluginDataDir, @NotNull Logger logger) {
        this(pluginDataDir, logger, DEFAULT_LANGUAGE);
    }

    public ConfigBootstrap(@NotNull Path pluginDataDir, @NotNull Logger logger, @NotNull String defaultLanguage) {
        this.pluginDataDir = pluginDataDir;
        this.logger = logger;
        this.defaultLanguage = defaultLanguage;
    }

    @NotNull
    public PluginConfig bootstrap() {
        PluginConfig pluginConfig = new PluginConfig(
            new PluginConfigLoader(pluginDataDir, new PluginConfigMigrator(logger))
        );
        boolean isFirstSetup = !Files.exists(pluginDataDir.resolve(PluginConfigLoader.CONFIG_NAME));
        FileConfiguration yamlConfig = pluginConfig.reload();
        boolean changed = false;
        if (isFirstSetup) {
            changed = applyFirstSetupLanguageSelection(yamlConfig);
        }
        if (changed) {
            try {
                pluginConfig.save();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to save config.yml after bootstrap language selection", e);
            }
        }
        I18nUtil.initialize(pluginDataDir.toFile(), resolveLocale(yamlConfig));
        logLanguageSuggestionIfUsingDefaultLanguage(yamlConfig);
        return pluginConfig;
    }

    private boolean applyFirstSetupLanguageSelection(@NotNull FileConfiguration pluginConfig) {
        String effectiveConfiguredLanguage = resolveConfiguredLanguage(pluginConfig.getString("language"));
        Locale systemLocale = Locale.getDefault();
        Optional<String> supportedSystemLocale = I18nUtil.resolveSupportedLocaleKey(systemLocale);
        if (supportedSystemLocale.isPresent() && !supportedSystemLocale.get().equalsIgnoreCase(effectiveConfiguredLanguage)) {
            String selectedLanguage = supportedSystemLocale.get();
            pluginConfig.set("language", selectedLanguage);
            logger.log(Level.INFO, "First setup: selected language '{0}' from system locale '{1}'.",
                new Object[]{selectedLanguage, systemLocale});
            logger.log(Level.INFO, "Use '/usb lang [locale]' to change language. Help improve translations: {0}",
                I18nUtil.getTranslationSupportUrl());
            return true;
        } else if (supportedSystemLocale.isPresent()) {
            logger.log(Level.INFO, "First setup: keeping configured language '{0}' (matches system locale).",
                effectiveConfiguredLanguage);
        } else {
            logger.log(Level.INFO, "First setup: keeping configured language '{0}' (system locale '{1}' not supported).",
                new Object[]{effectiveConfiguredLanguage, systemLocale});
        }
        logger.log(Level.INFO, "Use '/usb lang [locale]' to change language. Help improve translations: {0}",
            I18nUtil.getTranslationSupportUrl());
        return false;
    }

    private void logLanguageSuggestionIfUsingDefaultLanguage(@NotNull FileConfiguration pluginConfig) {
        String effectiveConfiguredLanguage = resolveConfiguredLanguage(pluginConfig.getString("language"));
        if (!defaultLanguage.equalsIgnoreCase(effectiveConfiguredLanguage)) {
            return;
        }

        Locale systemLocale = Locale.getDefault();
        Optional<String> supportedSystemLocale = I18nUtil.resolveSupportedLocaleKey(systemLocale);
        if (supportedSystemLocale.isPresent() && !defaultLanguage.equalsIgnoreCase(supportedSystemLocale.get())) {
            String suggestedLocale = supportedSystemLocale.get();
            logger.log(Level.INFO, "Language hint: configured language is '{0}', but system locale '{1}' is supported as '{2}'.",
                new Object[]{defaultLanguage, systemLocale, suggestedLocale});
            logger.log(Level.INFO, "Use '/usb lang {0}' to switch. Help improve translations: {1}",
                new Object[]{suggestedLocale, I18nUtil.getTranslationSupportUrl()});
        }
    }

    @NotNull
    private Locale resolveLocale(@NotNull FileConfiguration pluginConfig) {
        Locale configuredLocale = I18nUtil.getLocale(resolveConfiguredLanguage(pluginConfig.getString("language")));
        return configuredLocale != null ? configuredLocale : Locale.ENGLISH;
    }

    @NotNull
    private String resolveConfiguredLanguage(String configuredLanguage) {
        if (configuredLanguage == null || configuredLanguage.isBlank()) {
            return defaultLanguage;
        }
        return I18nUtil.findSupportedLocaleKey(configuredLanguage)
            .orElseGet(() -> {
                logger.log(Level.WARNING, "Config value 'language' references unsupported language '{0}'. Using fallback '{1}'.",
                    new Object[]{configuredLanguage, defaultLanguage});
                return defaultLanguage;
            });
    }
}
