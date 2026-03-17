package us.talabrek.ultimateskyblock.challenge;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;
import us.talabrek.ultimateskyblock.island.IslandKey;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChallengeCompletionLogicTest {
    @TempDir
    Path tempDir;

    @Test
    public void importsLegacyPlayerChallengesDuringStartup() throws Exception {
        ChallengeKey challengeKey = ChallengeKey.of("cobblestonegenerator");
        writeLegacyPlayerProgress("player-one.yml", 0, 64, 0, challengeKey, 2, 1, 1000L);

        try (ChallengeProgressRepository repository = new SqliteChallengeProgressRepository(
            tempDir.resolve("data").resolve("challenge-progress.db"),
            Logger.getAnonymousLogger()
        )) {
            ChallengeLogic challengeLogic = challengeLogic(challengeKey);
            ChallengeCompletionLogic logic = new ChallengeCompletionLogic(challengeLogic, plugin(challengeLogic), runtimeConfigs(), challengeConfig("player"), repository);

            Map<ChallengeKey, ChallengeCompletion> loaded = logic.getIslandChallenges("0,0");

            assertEquals(2, loaded.get(challengeKey).getTimesCompleted());
            assertEquals(1, loaded.get(challengeKey).getTimesCompletedInCooldown());
            assertNull(YamlConfiguration.loadConfiguration(tempDir.resolve("players/player-one.yml").toFile()).get("player.challenges"));
            assertTrue(repository.hasProgress(IslandKey.fromIslandName("0,0")));
            assertEquals("true", repository.getMetadata("legacy_yaml_import_completed").orElseThrow());
        }
    }

    @Test
    public void importsLegacyCompletionFilesDuringStartup() throws Exception {
        ChallengeKey challengeKey = ChallengeKey.of("cobblestonegenerator");
        UUID leaderUuid = UUID.randomUUID();
        long futureCooldown = System.currentTimeMillis() + 60_000L;
        writeLegacyIsland(leaderUuid, "0,0");
        writeLegacyCompletionFile(leaderUuid + ".yml", challengeKey, 3, 2, futureCooldown);

        try (ChallengeProgressRepository repository = new SqliteChallengeProgressRepository(
            tempDir.resolve("data").resolve("challenge-progress.db"),
            Logger.getAnonymousLogger()
        )) {
            ChallengeLogic challengeLogic = challengeLogic(challengeKey);
            ChallengeCompletionLogic logic = new ChallengeCompletionLogic(challengeLogic, plugin(challengeLogic), runtimeConfigs(), challengeConfig("island"), repository);

            Map<ChallengeKey, ChallengeCompletion> loaded = logic.getIslandChallenges("0,0");

            assertEquals(3, loaded.get(challengeKey).getTimesCompleted());
            assertEquals(2, loaded.get(challengeKey).getTimesCompletedInCooldown());
            assertFalse(Files.exists(tempDir.resolve("completion/" + leaderUuid + ".yml")));
            assertFalse(Files.exists(tempDir.resolve("completion")));
            assertTrue(Files.exists(tempDir.resolve("backup/completion/" + leaderUuid + ".yml")));
            assertEquals("true", repository.getMetadata("legacy_yaml_import_completed").orElseThrow());
        }
    }

    @Test
    public void keepsLegacyCompletionDirWhenOtherFilesRemain() throws Exception {
        ChallengeKey challengeKey = ChallengeKey.of("cobblestonegenerator");
        UUID leaderUuid = UUID.randomUUID();
        long futureCooldown = System.currentTimeMillis() + 60_000L;
        writeLegacyIsland(leaderUuid, "0,0");
        writeLegacyCompletionFile(leaderUuid + ".yml", challengeKey, 3, 2, futureCooldown);
        Path sentinel = tempDir.resolve("completion").resolve("README.txt");
        Files.writeString(sentinel, "keep");

        try (ChallengeProgressRepository repository = new SqliteChallengeProgressRepository(
            tempDir.resolve("data").resolve("challenge-progress.db"),
            Logger.getAnonymousLogger()
        )) {
            ChallengeLogic challengeLogic = challengeLogic(challengeKey);
            new ChallengeCompletionLogic(challengeLogic, plugin(challengeLogic), runtimeConfigs(), challengeConfig("island"), repository);

            assertTrue(Files.exists(tempDir.resolve("completion")));
            assertTrue(Files.exists(sentinel));
            assertTrue(Files.exists(tempDir.resolve("backup/completion/" + leaderUuid + ".yml")));
        }
    }

    private ChallengeLogic challengeLogic(ChallengeKey challengeKey) {
        ChallengeLogic challengeLogic = mock(ChallengeLogic.class);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<ChallengeKey, ChallengeCompletion> map = invocation.getArgument(0);
            map.put(challengeKey, new ChallengeCompletion(challengeKey, null, 0, 0));
            return null;
        }).when(challengeLogic).populateChallenges(org.mockito.ArgumentMatchers.any());
        return challengeLogic;
    }

    private uSkyBlock plugin(ChallengeLogic challengeLogic) {
        uSkyBlock plugin = mock(uSkyBlock.class);
        when(plugin.getChallengeLogic()).thenReturn(challengeLogic);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        return plugin;
    }

    private RuntimeConfigs runtimeConfigs() {
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig());
        return runtimeConfigs;
    }

    private static YamlConfiguration challengeConfig(String challengeSharing) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("challengeSharing", challengeSharing);
        return config;
    }

    private void writeLegacyPlayerProgress(
        String fileName,
        int islandX,
        int islandY,
        int islandZ,
        ChallengeKey challengeKey,
        int timesCompleted,
        int timesCompletedSinceTimer,
        long firstCompleted
    ) throws Exception {
        Path playersDir = tempDir.resolve("players");
        Files.createDirectories(playersDir);
        YamlConfiguration playerConfig = new YamlConfiguration();
        playerConfig.set("player.islandX", islandX);
        playerConfig.set("player.islandY", islandY);
        playerConfig.set("player.islandZ", islandZ);
        playerConfig.set("player.challenges." + challengeKey.id() + ".firstCompleted", firstCompleted);
        playerConfig.set("player.challenges." + challengeKey.id() + ".timesCompleted", timesCompleted);
        playerConfig.set("player.challenges." + challengeKey.id() + ".timesCompletedSinceTimer", timesCompletedSinceTimer);
        playerConfig.save(playersDir.resolve(fileName).toFile());
    }

    private void writeLegacyIsland(UUID leaderUuid, String islandName) throws Exception {
        Path islandsDir = tempDir.resolve("islands");
        Files.createDirectories(islandsDir);
        YamlConfiguration islandConfig = new YamlConfiguration();
        islandConfig.set("party.leader-uuid", leaderUuid.toString());
        islandConfig.save(islandsDir.resolve(islandName + ".yml").toFile());
    }

    private void writeLegacyCompletionFile(
        String fileName,
        ChallengeKey challengeKey,
        int timesCompleted,
        int timesCompletedSinceTimer,
        long firstCompleted
    ) throws Exception {
        Path completionDir = tempDir.resolve("completion");
        Files.createDirectories(completionDir);
        YamlConfiguration config = new YamlConfiguration();
        config.set(challengeKey.id() + ".firstCompleted", firstCompleted);
        config.set(challengeKey.id() + ".timesCompleted", timesCompleted);
        config.set(challengeKey.id() + ".timesCompletedSinceTimer", timesCompletedSinceTimer);
        config.save(completionDir.resolve(fileName).toFile());
    }

    private static RuntimeConfig runtimeConfig() {
        ItemStackSpec tool = new ItemStackSpec(new org.bukkit.inventory.ItemStack(Material.STONE));
        return new RuntimeConfig(
            "en",
            Locale.ENGLISH,
            new RuntimeConfig.Init(Duration.ZERO),
            new RuntimeConfig.General(4, "skyworld", Duration.ZERO, Duration.ZERO, Duration.ZERO, "plains", "nether_wastes", 64, Duration.ZERO),
            new RuntimeConfig.Island(
                128, 150, false, 128, 64, List.of(), true, Map.of(), true, true, true, "default",
                Duration.ZERO, Duration.ZERO, false, Duration.ZERO, 0.5d, Duration.ZERO, true, 10, true, "",
                new RuntimeConfig.SpawnLimits(true, 64, 50, 16, 5, 0), Map.of()
            ),
            new RuntimeConfig.Extras(false, true, true),
            new RuntimeConfig.Protection(true, true, true, true, true, true, true, true, true, true, true, true, false, false, true, true, true, false, false, false, true),
            new RuntimeConfig.Nether(false, 7, 75, "", new RuntimeConfig.Terraform(false, 0d, 0d, 0, Map.of(), Map.of()), new RuntimeConfig.SpawnChances(false, 0d, 0d, 0d)),
            new RuntimeConfig.Restart(true, true, true, true, false, true, Duration.ZERO, List.of()),
            new RuntimeConfig.Advanced(Duration.ZERO, false, 0d, true, "", "", "", "maximumSize=100", Duration.ZERO, Duration.ZERO, "", 4, Duration.ZERO, 0d, Duration.ZERO, null,
                new RuntimeConfig.PlayerDb("bukkit", "", "", Duration.ZERO)),
            new RuntimeConfig.Async(Duration.ZERO, 0L, Duration.ZERO),
            new RuntimeConfig.AsyncWorldEdit(false, Duration.ZERO, Duration.ZERO),
            new RuntimeConfig.Party(Duration.ZERO, "", List.of(), List.of(), Map.of()),
            new RuntimeConfig.PluginUpdates(true, "RELEASE"),
            new RuntimeConfig.Spawning(new RuntimeConfig.Guardians(false, 0, 0d), new RuntimeConfig.Phantoms(true, false)),
            new RuntimeConfig.Placeholder(false, false, false),
            new RuntimeConfig.ToolMenu(false, tool, List.of()),
            new RuntimeConfig.Signs(true),
            new RuntimeConfig.WorldGuard(true, true),
            new RuntimeConfig.Importer(0.1d, Duration.ZERO),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of()
        );
    }
}
