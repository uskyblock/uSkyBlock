package us.talabrek.ultimateskyblock.imports;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ItemComponentConverterTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testMinimalChallengeConversion() throws Exception {
        var testFile = new File(testFolder.getRoot(), "challenges.yml");
        try (var reader = getClass().getResourceAsStream("test-challenges.yml")) {
            Files.copy(reader, testFile.toPath());
        }

        var plugin = mock(uSkyBlock.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());

        var converter = new ItemComponentConverter();
        converter.init(plugin);
        converter.importFile(testFile);

        assertTrue(testFile.exists());
        File backup = new File(testFolder.getRoot(), "challenges.yml.old");
        assertTrue(backup.isFile());

        YamlConfiguration actual = new YamlConfiguration();
        actual.load(testFile);
        YamlConfiguration expected = new YamlConfiguration();
        try (var reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("test-challenges-expected.yml"), StandardCharsets.UTF_8))) {
            expected.load(reader);
        }
        assertConfigsEquals(expected, actual);
    }

    @Test
    public void testDefaultChallengeConversion() throws Exception {
        var testFile = new File(testFolder.getRoot(), "challenges.yml");
        try (var reader = getClass().getResourceAsStream("old-default-challenges.yml")) {
            Files.copy(reader, testFile.toPath());
        }

        var plugin = mock(uSkyBlock.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());

        var converter = new ItemComponentConverter();
        converter.init(plugin);
        converter.importFile(testFile);

        assertTrue(testFile.exists());
        File backup = new File(testFolder.getRoot(), "challenges.yml.old");
        assertTrue(backup.isFile());

        YamlConfiguration actual = new YamlConfiguration();
        actual.load(testFile);
        YamlConfiguration expected = new YamlConfiguration();
        try (var reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("old-default-challenges-expected.yml"), StandardCharsets.UTF_8))) {
            expected.load(reader);
        }
        assertConfigsEquals(expected, actual);
    }

    private void assertConfigsEquals(YamlConfiguration expected, YamlConfiguration actual) {
        for (String key : expected.getKeys(true)) {
            assertTrue("Missing key: " + key, actual.contains(key));
            if (expected.isConfigurationSection(key)) {
                assertTrue("Key should be a section: " + key, actual.isConfigurationSection(key));
            } else {
                assertEquals("Items mismatch at key: " + key, expected.get(key), actual.get(key));
            }
            assertThat("Comments mismatch at key: " + key, actual.getComments(key), is(expected.getComments(key)));
            assertThat("Inline comments mismatch at key: " + key, actual.getInlineComments(key), is(expected.getInlineComments(key)));
        }
        assertThat("Headers should be the same", actual.options().getHeader(), is(expected.options().getHeader()));
        assertThat("Footers should be the same", actual.options().getFooter(), is(expected.options().getFooter()));
    }
}
