package us.talabrek.ultimateskyblock.placeholder;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlaceholderIntegrationsTest {

    private TestLogHandler logHandler;
    private PlaceholderIntegrations integrations;

    @BeforeEach
    public void setUp() {
        RecordingIntegration.enabledWith.clear();
        PlaceholderSource source = mock(PlaceholderSource.class);
        logHandler = new TestLogHandler();
        integrations = new PlaceholderIntegrations(source, new PlaceholderService(source), testLogger(logHandler));
    }

    @Test
    public void enablesIntegrationWhenBackingPluginIsEnabled() {
        uSkyBlock plugin = mock(uSkyBlock.class, RETURNS_DEEP_STUBS);
        when(plugin.getServer().getPluginManager().isPluginEnabled("RecordingTestPlugin")).thenReturn(true);

        integrations.enableIntegrations(plugin);

        assertEquals(List.of(plugin), RecordingIntegration.enabledWith);
        assertTrue(logHandler.messages().stream()
            .anyMatch(message -> message.contains("Enabled placeholder integration: RecordingTestPlugin")));
    }

    @Test
    public void skipsIntegrationWhenBackingPluginIsNotEnabled() {
        uSkyBlock plugin = mock(uSkyBlock.class, RETURNS_DEEP_STUBS);
        when(plugin.getServer().getPluginManager().isPluginEnabled("RecordingTestPlugin")).thenReturn(false);

        integrations.enableIntegrations(plugin);

        assertEquals(List.of(), RecordingIntegration.enabledWith);
    }

    /**
     * Discovered via the service file in
     * {@code src/test/resources/META-INF/services/us.talabrek.ultimateskyblock.placeholder.PlaceholderIntegration}.
     * ServiceLoader instantiates a fresh provider per load, so enable calls are recorded statically.
     */
    public static final class RecordingIntegration implements PlaceholderIntegration {
        private static final List<uSkyBlock> enabledWith = new ArrayList<>();

        @Override
        public @NotNull String pluginName() {
            return "RecordingTestPlugin";
        }

        @Override
        public void enable(@NotNull PlaceholderSource source, @NotNull PlaceholderService service, @NotNull uSkyBlock plugin) {
            enabledWith.add(plugin);
        }
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
