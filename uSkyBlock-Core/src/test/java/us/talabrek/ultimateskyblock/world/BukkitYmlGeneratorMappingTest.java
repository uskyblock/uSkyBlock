package us.talabrek.ultimateskyblock.world;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BukkitYmlGeneratorMappingTest {

    @TempDir
    Path tempDir;

    private Path bukkitYml;
    private BukkitYmlGeneratorMapping mapping;
    private TestLogHandler logHandler;

    @BeforeEach
    public void setUp() {
        bukkitYml = tempDir.resolve("bukkit.yml");
        logHandler = new TestLogHandler();
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(logHandler);
        mapping = new BukkitYmlGeneratorMapping(bukkitYml, logger);
    }

    @Test
    public void addsMappingWhenMissing() throws IOException {
        Files.writeString(bukkitYml, """
            settings:
              shutdown-message: Server closed
            """);

        mapping.ensureMapping("skyworld");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(bukkitYml.toFile());
        assertThat(config.getString("worlds.skyworld.generator"), is("uSkyBlock"));
        assertThat(config.getString("settings.shutdown-message"), is("Server closed"));
    }

    @Test
    public void preservesCommentsOnSave() throws IOException {
        Files.writeString(bukkitYml, """
            # This is the Bukkit configuration file
            settings:
              shutdown-message: Server closed
            """);

        mapping.ensureMapping("skyworld");

        assertThat(Files.readString(bukkitYml), containsString("# This is the Bukkit configuration file"));
    }

    @Test
    public void leavesForeignGeneratorUntouched() throws IOException {
        Files.writeString(bukkitYml, """
            worlds:
              skyworld:
                generator: VoidGen
            """);

        mapping.ensureMapping("skyworld");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(bukkitYml.toFile());
        assertThat(config.getString("worlds.skyworld.generator"), is("VoidGen"));
        assertThat(logHandler.warnings(), containsString("VoidGen"));
    }

    @Test
    public void keepsMatchingMappingWithoutRewrite() throws IOException {
        String content = """
            worlds:
              skyworld:
                generator: uSkyBlock
            """;
        Files.writeString(bukkitYml, content);

        mapping.ensureMapping("skyworld");

        assertThat(Files.readString(bukkitYml), is(content));
    }

    @Test
    public void unparseableFileIsLeftUntouchedWithWarning() throws IOException {
        String content = "worlds:\n\t- this is not valid yaml\n";
        Files.writeString(bukkitYml, content);

        mapping.ensureMapping("skyworld");

        assertThat(Files.readString(bukkitYml), is(content));
        assertThat(logHandler.warnings(), containsString("bukkit.yml"));
    }

    @Test
    public void missingFileIsSkippedWithWarning() {
        mapping.ensureMapping("skyworld");

        assertFalse(Files.exists(bukkitYml));
        assertThat(logHandler.warnings(), containsString("bukkit.yml"));
    }

    private static final class TestLogHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        String warnings() {
            return records.stream()
                .filter(record -> record.getLevel() == Level.WARNING)
                .map(LogRecord::getMessage)
                .collect(Collectors.joining("\n"));
        }
    }
}
