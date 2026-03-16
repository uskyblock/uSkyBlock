package us.talabrek.ultimateskyblock.challenge;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChallengeCompletionLogicTest {
    @TempDir
    Path tempDir;

    @Test
    public void importsLegacyPlayerChallengesIntoRepository() {
        ChallengeProgressRepository repository = mock(ChallengeProgressRepository.class);
        when(repository.hasProgress(any())).thenReturn(false);
        when(repository.load(any())).thenReturn(Map.of());

        ChallengeLogic challengeLogic = mock(ChallengeLogic.class);
        ChallengeKey challengeKey = ChallengeKey.of("cobblestonegenerator");
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<ChallengeKey, ChallengeCompletion> map = invocation.getArgument(0);
            map.put(challengeKey, new ChallengeCompletion(challengeKey, null, 0, 0));
            return null;
        }).when(challengeLogic).populateChallenges(any());

        uSkyBlock plugin = mock(uSkyBlock.class);
        when(plugin.getChallengeLogic()).thenReturn(challengeLogic);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());

        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig());

        YamlConfiguration config = new YamlConfiguration();
        config.set("challengeSharing", "player");

        ChallengeCompletionLogic logic = new ChallengeCompletionLogic(plugin, runtimeConfigs, config, repository);

        PlayerInfo playerInfo = mock(PlayerInfo.class);
        when(playerInfo.getHasIsland()).thenReturn(true);
        when(playerInfo.locationForParty()).thenReturn("0,0");
        when(playerInfo.getPlayerName()).thenReturn("TestPlayer");

        YamlConfiguration playerConfig = new YamlConfiguration();
        playerConfig.set("player.challenges.cobblestonegenerator.firstCompleted", 1000L);
        playerConfig.set("player.challenges.cobblestonegenerator.timesCompleted", 2);
        playerConfig.set("player.challenges.cobblestonegenerator.timesCompletedSinceTimer", 1);
        when(playerInfo.getConfig()).thenReturn(playerConfig);

        Map<ChallengeKey, ChallengeCompletion> loaded = logic.getChallenges(playerInfo);

        assertEquals(2, loaded.get(challengeKey).getTimesCompleted());
        assertEquals(1, loaded.get(challengeKey).getTimesCompletedInCooldown());
        assertNull(playerConfig.get("player.challenges"));
        verify(playerInfo).save();

        ArgumentCaptor<Map<ChallengeKey, ChallengeCompletion>> captor = ArgumentCaptor.forClass(Map.class);
        verify(repository).replace(any(), captor.capture());
        assertEquals(2, captor.getValue().get(challengeKey).getTimesCompleted());
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
