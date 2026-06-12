package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeProperties;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards;
import us.talabrek.ultimateskyblock.challenge.catalog.DisplaySpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RepeatPolicy;
import us.talabrek.ultimateskyblock.challenge.catalog.RewardBundle;
import us.talabrek.ultimateskyblock.challenge.catalog.TextSpec;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;
import us.talabrek.ultimateskyblock.island.IslandInfo;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IslandBiomeUnlocksTest {
    private static final ChallengeId CHALLENGE_ID = ChallengeId.of("fisherman");

    private final ChallengeLogic challengeLogic = mock(ChallengeLogic.class);
    private final ChallengeCompletionLogic completionLogic = mock(ChallengeCompletionLogic.class);
    private final RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
    private final IslandInfo island = mock(IslandInfo.class);

    private IslandBiomeUnlocks biomeUnlocks;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
        when(runtimeConfigs.current()).thenReturn(runtimeConfig());
        when(island.getName()).thenReturn("0,0");

        var field = ChallengeLogic.class.getDeclaredField("completionLogic");
        field.setAccessible(true);
        field.set(challengeLogic, completionLogic);

        biomeUnlocks = new IslandBiomeUnlocks(challengeLogic, runtimeConfigs);
    }

    @Test
    public void derivesUnlocksFromCompletedChallengeRewards() {
        Map<ChallengeId, ChallengeCompletion> progress = new HashMap<>();
        progress.put(CHALLENGE_ID, new ChallengeCompletion(CHALLENGE_ID, null, 1, 0));
        // Build before stubbing: construction touches the mocked server's item factory.
        Optional<ChallengeDefinition> challenge = Optional.of(biomeRewardChallenge("deep_ocean"));
        when(completionLogic.getIslandChallenges("0,0")).thenReturn(progress);
        when(challengeLogic.getDefinitionById(CHALLENGE_ID)).thenReturn(challenge);

        assertEquals(Set.of("ocean", "deep_ocean"), biomeUnlocks.unlockedBiomes(island));
        assertTrue(biomeUnlocks.isUnlocked(island, "DEEP_OCEAN"));
    }

    @Test
    public void uncompletedChallengesUnlockNothing() {
        Map<ChallengeId, ChallengeCompletion> progress = new HashMap<>();
        progress.put(CHALLENGE_ID, new ChallengeCompletion(CHALLENGE_ID, null, 0, 0));
        when(completionLogic.getIslandChallenges("0,0")).thenReturn(progress);

        assertEquals(Set.of("ocean"), biomeUnlocks.unlockedBiomes(island));
        assertFalse(biomeUnlocks.isUnlocked(island, "deep_ocean"));
    }

    @Test
    public void permissionRemainsAnOrFallback() {
        when(completionLogic.getIslandChallenges(any())).thenReturn(new HashMap<>());
        Player player = mock(Player.class);
        when(player.hasPermission("usb.biome.jungle")).thenReturn(true);

        assertTrue(biomeUnlocks.canUseBiome(player, island, "Jungle"));
        assertFalse(biomeUnlocks.canUseBiome(player, island, "deep_ocean"));
        assertTrue(biomeUnlocks.canUseBiome(player, island, "ocean"));
    }

    private static RuntimeConfig runtimeConfig() {
        ItemStackSpec tool = new ItemStackSpec(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE));
        return new RuntimeConfig(
            "en",
            java.util.Locale.ENGLISH,
            new RuntimeConfig.Init(Duration.ZERO),
            new RuntimeConfig.General(4, "skyworld", Duration.ZERO, Duration.ZERO, Duration.ZERO, "plains", "nether_wastes", 64, Duration.ZERO),
            new RuntimeConfig.Island(
                128, 150, false, 128, 64, List.of(), true, Map.of(), true, true, true, "default",
                Duration.ZERO, Duration.ZERO, false, Duration.ZERO, 0.5d, Duration.ZERO, true, 10, true, "",
                new RuntimeConfig.SpawnLimits(true, 64, 50, 16, 5, 0), Map.of()
            ),
            new RuntimeConfig.Extras(false, true, true),
            new RuntimeConfig.Protection(true, true, true, true, true, true, true, true, true, true, true, true, false, false, true, true, true, false, false, false, true),
            new RuntimeConfig.Challenges(true, true, true, new RuntimeConfig.Broadcast(true, "")),
            new RuntimeConfig.Biomes(List.of("Ocean")),
            new RuntimeConfig.Nether(false, 7, 75, "", new RuntimeConfig.Terraform(false, 0d, 0d, 0, Map.of(), Map.of()), new RuntimeConfig.SpawnChances(false, 0d, 0d, 0d)),
            new RuntimeConfig.Restart(true, true, true, true, false, true, Duration.ZERO, List.of()),
            new RuntimeConfig.Advanced(Duration.ZERO, false, 0d, true, "", "", Duration.ZERO, Duration.ZERO, "", 4, Duration.ZERO, 0d, Duration.ZERO, null,
                new RuntimeConfig.PlayerDb("bukkit", "", "", Duration.ZERO)),
            new RuntimeConfig.Async(Duration.ZERO, 0L, Duration.ZERO),
            new RuntimeConfig.AsyncWorldEdit(false, Duration.ZERO, Duration.ZERO),
            new RuntimeConfig.Party(Duration.ZERO, "", List.of(), List.of(), Map.of()),
            new RuntimeConfig.PluginUpdates(true, "RELEASE"),
            new RuntimeConfig.Spawning(new RuntimeConfig.Guardians(false, 0, 0d), new RuntimeConfig.Phantoms(true, false)),
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

    private ChallengeDefinition biomeRewardChallenge(String biome) {
        ItemStackSpec stone = new GameObjectFactory().itemStack("minecraft:stone");
        return new ChallengeDefinition(
            CHALLENGE_ID,
            new DisplaySpec(TextSpec.miniMessage("Fisherman"), TextSpec.empty(), stone),
            stone,
            List.of(),
            List.of(),
            new ChallengeProperties(true),
            new RepeatPolicy(false, Duration.ZERO, 0),
            new RewardBundle(List.of(new ChallengeRewards.BiomeReward(List.of(biome)))),
            RewardBundle.empty()
        );
    }
}
