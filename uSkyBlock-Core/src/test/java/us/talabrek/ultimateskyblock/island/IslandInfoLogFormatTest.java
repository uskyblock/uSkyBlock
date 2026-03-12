package us.talabrek.ultimateskyblock.island;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.text.DateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;

public class IslandInfoLogFormatTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUpI18n() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
    }

    @Test
    public void getLogDeserializesV2EntriesWithSemicolons() throws Exception {
        long then = Instant.now().minusSeconds(5).toEpochMilli();
        IslandInfo islandInfo = createIslandInfo(config -> config.set("log", List.of(
            then + ";v2;Player joined;with semicolon"
        )));

        List<String> log = islandInfo.getLog();

        assertThat(log.size(), is(1));
        assertThat(log.get(0), containsString("Player joined;with semicolon"));
    }

    @Test
    public void getLogDeserializesLegacyMessageFormatEntries() throws Exception {
        long then = Instant.now().minusSeconds(5).toEpochMilli();
        IslandInfo islandInfo = createIslandInfo(config -> config.set("log", List.of(
            then + ";\u00a7aUser {0} joined.;Bob"
        )));

        List<String> log = islandInfo.getLog();

        assertThat(log.size(), is(1));
        assertThat(log.get(0), containsString("User Bob joined."));
        assertThat(log.get(0), not(containsString("\u00a7aUser Bob joined.")));
    }

    @Test
    public void getLogDeserializesVeryOldBracketFormatEntries() throws Exception {
        String date = DateFormat.getDateInstance(DateFormat.SHORT)
            .format(Date.from(Instant.now().minus(Duration.ofDays(1))));
        IslandInfo islandInfo = createIslandInfo(config -> config.set("log", List.of(
            "\u00a7d[" + date + "]\u00a77 \u00a7aLegacy event"
        )));

        List<String> log = islandInfo.getLog();

        assertThat(log.size(), is(1));
        assertThat(log.get(0), containsString("Legacy event"));
        assertThat(log.get(0), not(containsString("\u00a7aLegacy event")));
    }

    @Test
    public void getLogReadsLegacyRingBufferEntries() throws Exception {
        long then = Instant.now().minusSeconds(5).toEpochMilli();
        IslandInfo islandInfo = createIslandInfo(config -> {
            config.set("log.logPos", 1);
            config.set("log.2", then + ";v2;First");
            config.set("log.3", then + ";v2;Second");
        });

        List<String> log = islandInfo.getLog();

        assertThat(log.size(), is(2));
        assertThat(log.get(0), containsString("First"));
        assertThat(log.get(1), containsString("Second"));
    }

    private IslandInfo createIslandInfo(ConfigCustomizer customizer) throws Exception {
        String islandName = "island";
        File islandDir = tempDir.resolve("island-info").toFile();
        islandDir.mkdirs();
        File islandConfigFile = new File(islandDir, islandName + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", 3);
        customizer.apply(config);
        config.save(islandConfigFile);
        return new IslandInfo(islandName, mock(uSkyBlock.class), islandDir.toPath());
    }

    @FunctionalInterface
    private interface ConfigCustomizer {
        void apply(YamlConfiguration config) throws Exception;
    }
}
