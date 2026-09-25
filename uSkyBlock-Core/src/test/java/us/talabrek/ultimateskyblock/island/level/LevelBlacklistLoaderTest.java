package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LevelBlacklistLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    public void bundledBlacklistResolvesEveryNameOnTheCompileApi() throws Exception {
        List<LogRecord> records = new ArrayList<>();
        Set<Material> ignored = new LevelBlacklistLoader(tempDir, capturingLogger(records)).load();

        assertTrue(ignored.containsAll(Set.of(Material.AIR, Material.CAVE_AIR, Material.VOID_AIR,
            Material.WATER, Material.LAVA, Material.FIRE, Material.NETHER_PORTAL, Material.BEDROCK)));
        assertFalse(ignored.contains(Material.STONE));
        // Every bundled name must exist on the API the plugin is compiled against: a typo would silently
        // shrink the list on real servers.
        assertEquals(bundledNames().size(), ignored.size(), "every bundled name resolves to a Material");
        assertTrue(records.stream().noneMatch(record -> record.getLevel() == Level.WARNING),
            "no unknown-name warning for the bundled list");
        assertFalse(Files.exists(tempDir.resolve(LevelBlacklistLoader.CONFIG_DIRECTORY)));
    }

    @Test
    public void dataFolderFileReplacesTheBundledListAndSkipsUnknownNames() throws Exception {
        Path configDir = Files.createDirectories(tempDir.resolve(LevelBlacklistLoader.CONFIG_DIRECTORY));
        Files.writeString(configDir.resolve(LevelBlacklistLoader.LEVEL_BLACKLIST_NAME),
            "ignore:\n  - stone\n  - minecraft:dirt\n  - not_a_block\n");
        List<LogRecord> records = new ArrayList<>();

        Set<Material> ignored = new LevelBlacklistLoader(tempDir, capturingLogger(records)).load();

        assertEquals(Set.of(Material.STONE, Material.DIRT), ignored);
        List<LogRecord> warnings = records.stream().filter(record -> record.getLevel() == Level.WARNING).toList();
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().getMessage().contains("not_a_block"));
    }

    @Test
    public void malformedOrMisKeyedOverrideFallsBackToTheBundledList() throws Exception {
        Path configDir = Files.createDirectories(tempDir.resolve(LevelBlacklistLoader.CONFIG_DIRECTORY));
        Path override = configDir.resolve(LevelBlacklistLoader.LEVEL_BLACKLIST_NAME);

        Files.writeString(override, "ignored:\n  - stone\n");
        List<LogRecord> records = new ArrayList<>();
        Set<Material> misKeyed = new LevelBlacklistLoader(tempDir, capturingLogger(records)).load();
        assertTrue(misKeyed.contains(Material.WATER), "bundled list used when the override has no 'ignore' list");
        assertFalse(misKeyed.contains(Material.STONE));
        assertEquals(1, records.stream().filter(record -> record.getLevel() == Level.WARNING).count());

        Files.writeString(override, "ignore: [\n");
        records.clear();
        Set<Material> malformed = new LevelBlacklistLoader(tempDir, capturingLogger(records)).load();
        assertTrue(malformed.contains(Material.WATER), "bundled list used when the override cannot be parsed");
        assertEquals(1, records.stream().filter(record -> record.getLevel() == Level.WARNING).count());
    }

    private static List<String> bundledNames() throws Exception {
        try (var stream = Objects.requireNonNull(LevelBlacklistLoaderTest.class.getClassLoader()
                 .getResourceAsStream(LevelBlacklistLoader.LEVEL_BLACKLIST_NAME));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader).getStringList("ignore");
        }
    }

    private static Logger capturingLogger(List<LogRecord> records) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
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
        });
        return logger;
    }
}
