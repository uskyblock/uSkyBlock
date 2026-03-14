package us.talabrek.ultimateskyblock.config.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.config.PluginConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ConfigBootstrapTest {
    @TempDir
    Path tempDir;

    @Test
    void keepsConfiguredLanguageWhenConfigAlreadyExists() {
        Locale originalDefault = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            PluginConfig initialConfig = new ConfigBootstrap(tempDir, testLogger(new TestLogHandler()), "en").bootstrap();
            initialConfig.getYamlConfig().set("language", "en");
            initialConfig.save();

            Locale.setDefault(Locale.GERMAN);
            PluginConfig bootstrappedConfig = new ConfigBootstrap(tempDir, testLogger(new TestLogHandler()), "en").bootstrap();

            assertThat(bootstrappedConfig.getYamlConfig().getString("language"), is("en"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            Locale.setDefault(originalDefault);
        }
    }

    @Test
    void keepsInvalidConfiguredLanguageButBootstrapsEnglishLocale() {
        Locale originalDefault = Locale.getDefault();
        try {
            TestLogHandler logHandler = new TestLogHandler();
            Locale.setDefault(Locale.GERMAN);
            PluginConfig initialConfig = new ConfigBootstrap(tempDir, testLogger(new TestLogHandler()), "en").bootstrap();
            initialConfig.getYamlConfig().set("language", "definitely-not-a-real-locale");
            initialConfig.save();

            new ConfigBootstrap(tempDir, testLogger(logHandler), "en").bootstrap();

            assertThat(dk.lockfuglsang.minecraft.po.I18nUtil.getI18n().getLocale(), is(Locale.ENGLISH));
            assertThat(logHandler.messages().stream()
                .anyMatch(message -> message.contains("Config value 'language' references unsupported language")), is(true));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            Locale.setDefault(originalDefault);
        }
    }

    @Test
    void selectsSupportedSystemLanguageOnFirstSetup() {
        Locale supportedSystemLocale = findSupportedNonEnglishLocale();
        String expectedLanguage = dk.lockfuglsang.minecraft.po.I18nUtil.resolveSupportedLocaleKey(supportedSystemLocale)
            .orElseThrow();
        Locale originalDefault = Locale.getDefault();
        Locale.setDefault(supportedSystemLocale);
        try {
            PluginConfig pluginConfig = new ConfigBootstrap(tempDir, testLogger(new TestLogHandler()), "en").bootstrap();

            assertThat(pluginConfig.getYamlConfig().getString("language"), is(expectedLanguage));
        } finally {
            Locale.setDefault(originalDefault);
        }
    }

    private Locale findSupportedNonEnglishLocale() {
        List<Locale> candidates = List.of(Locale.GERMAN, Locale.FRENCH, Locale.forLanguageTag("da"), Locale.forLanguageTag("es"));
        Optional<Locale> supported = candidates.stream()
            .filter(locale -> dk.lockfuglsang.minecraft.po.I18nUtil.resolveSupportedLocaleKey(locale)
                .filter(key -> !"en".equalsIgnoreCase(key))
                .isPresent())
            .findFirst();
        assumeTrue(supported.isPresent(), "No supported non-English locale available for bootstrap test");
        return supported.orElseThrow();
    }

    private static Logger testLogger(TestLogHandler handler) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
        return logger;
    }

    private static final class TestLogHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        private List<String> messages() {
            return messages;
        }
    }
}
