package us.talabrek.ultimateskyblock.imports;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ItemComponentConverterTest {
    @TempDir
    Path tempDir;

    @Test
    public void testMinimalChallengeConversion() throws Exception {
        testConfig("test-challenges.yml", "test-challenges-expected.yml", "challenges.yml");
    }

    @Test
    public void testDefaultChallengeConversion() throws Exception {
        testConfig("old-default-challenges.yml", "old-default-challenges-expected.yml", "challenges.yml");
    }

    @Test
    public void testDefaultSettingsConversion() throws Exception {
        testConfig("old-config.yml", "old-config-expected.yml", "config.yml");
    }

    @Test
    public void testDefaultChallengeConversionBlockRequirements() throws Exception {
        testBlockConverterConfig("old-block-challenges.yml", "expected-block-challenges.yml", "challenges.yml");
    }

    private void testBlockConverterConfig(String originalName, String expectedName, String fileName) throws Exception {
        var testFile = tempDir.resolve(fileName).toFile();
        try (var reader = Objects.requireNonNull(getClass().getResourceAsStream(originalName))) {
            Files.copy(reader, testFile.toPath());
        }

        var converter = new BlockRequirementConverter(Logger.getAnonymousLogger());
        converter.importFile(testFile);

        assertTrue(testFile.exists());
        long backupFiles;
        try (var stream = Files.find(tempDir, 1,
            (path, attr) ->
                path.getFileName().toString().startsWith(fileName) && path.getFileName().toString().endsWith(".old")
        )) {
            backupFiles = stream.count();
        }
        assertEquals(1, backupFiles);

        YamlConfiguration actual = new YamlConfiguration();
        actual.load(testFile);
        YamlConfiguration expected = new YamlConfiguration();
        try (var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
            getClass().getResourceAsStream(expectedName)), StandardCharsets.UTF_8))) {
            expected.load(reader);
        }
        assertConfigsEquals(expected, actual);
    }

    private void testConfig(String originalName, String expectedName, String fileName) throws Exception {
        var testFile = tempDir.resolve(fileName).toFile();
        try (var reader = Objects.requireNonNull(getClass().getResourceAsStream(originalName))) {
            Files.copy(reader, testFile.toPath());
        }

        var converter = new ItemComponentConverter(Logger.getAnonymousLogger());
        converter.importFile(testFile);

        assertTrue(testFile.exists());
        File backup = tempDir.resolve(fileName + ".old").toFile();
        assertTrue(backup.isFile());

        YamlConfiguration actual = new YamlConfiguration();
        actual.load(testFile);
        YamlConfiguration expected = new YamlConfiguration();
        try (var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
            getClass().getResourceAsStream(expectedName)), StandardCharsets.UTF_8))) {
            expected.load(reader);
        }
        assertConfigsEquals(expected, actual);
    }

    private void assertConfigsEquals(YamlConfiguration expected, YamlConfiguration actual) {
        for (String key : expected.getKeys(true)) {
            assertTrue(actual.contains(key), "Missing key: " + key);
            if (expected.isConfigurationSection(key)) {
                assertTrue(actual.isConfigurationSection(key), "Key should be a section: " + key);
            } else {
                assertEquals(expected.get(key), actual.get(key), "Items mismatch at key: " + key);
            }
            assertThat("Comments mismatch at key: " + key, actual.getComments(key), is(expected.getComments(key)));
            assertThat("Inline comments mismatch at key: " + key, actual.getInlineComments(key), is(expected.getInlineComments(key)));
        }
        assertThat("Headers should be the same", actual.options().getHeader(), is(expected.options().getHeader()));
        assertThat("Footers should be the same", actual.options().getFooter(), is(expected.options().getFooter()));
    }
}
