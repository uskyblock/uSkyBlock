package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeProperties;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ChallengeUnlockRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletionRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.InventoryItemsRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.IslandLevelRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ItemAmountProgression;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ItemRequirementSpec;
import us.talabrek.ultimateskyblock.challenge.catalog.DisplaySpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDisplaySpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;
import us.talabrek.ultimateskyblock.challenge.catalog.RepeatPolicy;
import us.talabrek.ultimateskyblock.challenge.catalog.RewardBundle;
import us.talabrek.ultimateskyblock.challenge.catalog.TextSpec;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;
import us.talabrek.ultimateskyblock.island.IslandKey;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.io.File;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChallengeExecutorTest {
    private static final IslandKey ISLAND = IslandKey.fromIslandName("1,1");
    private static final ChallengeKey CHALLENGE_ID = ChallengeKey.of("testchallenge");

    private final ChallengeProgressRepository repository = mock(ChallengeProgressRepository.class);
    private final ChallengeLogic challengeLogic = mock(ChallengeLogic.class);
    private final RewardApplier rewardApplier = mock(RewardApplier.class);
    private final uSkyBlock plugin = mock(uSkyBlock.class);
    private final Player player = mock(Player.class);
    private final PlayerInfo playerInfo = mock(PlayerInfo.class);
    private final PlayerInventory playerInventory = mock(PlayerInventory.class);
    private final GameObjectFactory gameObjects = new GameObjectFactory();

    private ChallengeProgressCache progressCache;
    /**
     * Tasks handed to the mocked scheduler; drained after each executor call. Deferring (instead
     * of running inline) mirrors the real scheduler and avoids re-entrant cache updates.
     */
    private final Deque<Runnable> scheduledTasks = new ArrayDeque<>();
    private Scheduler scheduler;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        scheduler = deferringScheduler();
        progressCache = new ChallengeProgressCache(Logger.getAnonymousLogger(), scheduler, repository, challengeLogic);

        when(plugin.getPlayerInfo(player)).thenReturn(playerInfo);
        when(plugin.playerIsOnOwnIsland(player)).thenReturn(true);
        when(plugin.getIslandInfo(player)).thenReturn(null);
        when(playerInfo.getHasIsland()).thenReturn(true);
        when(playerInfo.locationForParty()).thenReturn("1,1");
        when(player.getName()).thenReturn("Tester");
        when(player.isOnline()).thenReturn(true);
        when(player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
        when(playerInventory.getContents()).thenReturn(new ItemStack[0]);
    }

    private ChallengeExecutor executor(ChallengeDefinition definition) {
        ChallengeCatalog catalog = new ChallengeCatalog(List.of(new RankDefinition(
            RankId.of("starter"),
            new RankDisplaySpec(TextSpec.miniMessage("Starter"), TextSpec.empty()),
            gameObjects.itemStack("minecraft:barrier"),
            List.of(),
            List.of(definition)
        )));
        when(challengeLogic.getDefinitionById(CHALLENGE_ID)).thenReturn(Optional.of(definition));
        return new ChallengeExecutor(Logger.getAnonymousLogger(), plugin, scheduler, challengeLogic,
            new ChallengeUnlockEvaluator(catalog), rewardApplier, progressCache, repository);
    }

    private ChallengeDefinition challenge(
        List<ChallengeUnlockRequirement> unlock,
        List<CompletionRequirement> completion,
        boolean consumeItems,
        boolean repeatable
    ) {
        ItemStackSpec stone = gameObjects.itemStack("minecraft:stone");
        return new ChallengeDefinition(
            ChallengeId.of(CHALLENGE_ID.id()),
            new DisplaySpec(TextSpec.miniMessage("Test Challenge"), TextSpec.empty(), stone),
            stone,
            unlock,
            completion,
            new ChallengeProperties(consumeItems),
            new RepeatPolicy(repeatable, Duration.ZERO, 0),
            RewardBundle.empty(),
            RewardBundle.empty()
        );
    }

    private ChallengeDefinition simpleChallenge() {
        return challenge(List.of(), List.of(), false, true);
    }

    @Test
    public void appliesRewardOnlyAfterSuccessfulPersist() {
        ChallengeExecutor executor = executor(simpleChallenge());
        progressCache.replaceLoaded(ISLAND, new HashMap<>());

        executor.attempt(player, CHALLENGE_ID);
        drainScheduledTasks();

        InOrder order = inOrder(repository, rewardApplier);
        order.verify(repository).replace(eq(ISLAND), any());
        order.verify(rewardApplier).apply(eq(player), eq(playerInfo), any(ChallengeDefinition.class), eq(true));
        LoadedChallengeProgress loaded = progressCache.getIfLoaded(ISLAND).orElseThrow();
        assertEquals(1, loaded.snapshot().get(CHALLENGE_ID).getTimesCompleted());
        assertFalse(loaded.isCompletionInFlight());
    }

    @Test
    public void withholdsRewardAndKeepsMemoryWhenPersistFails() {
        ChallengeExecutor executor = executor(simpleChallenge());
        progressCache.replaceLoaded(ISLAND, new HashMap<>());
        doThrow(new IllegalStateException("database unavailable")).when(repository).replace(any(), any());

        executor.attempt(player, CHALLENGE_ID);
        drainScheduledTasks();

        verify(rewardApplier, never()).apply(any(), any(), any(), anyBoolean());
        LoadedChallengeProgress loaded = progressCache.getIfLoaded(ISLAND).orElseThrow();
        // In-memory state must still match the database: the completion never happened.
        assertFalse(loaded.snapshot().containsKey(CHALLENGE_ID));
        assertTrue(loaded.isWriteLocked());
    }

    @Test
    public void enforcesChallengeUnlockRequirementsOnCommandPath() {
        // Island level 0 (no island info stubbed) < 10: the attempt must be rejected.
        ChallengeExecutor executor = executor(challenge(
            List.of(new IslandLevelRequirement(10)), List.of(), false, true));
        progressCache.replaceLoaded(ISLAND, new HashMap<>());

        executor.attempt(player, CHALLENGE_ID);
        drainScheduledTasks();

        verify(repository, never()).replace(any(), any());
        assertFalse(progressCache.getIfLoaded(ISLAND).orElseThrow().isCompletionInFlight());
    }

    @Test
    public void evaluatesMixedCompletionRequirementKinds() {
        // Items + island level in one challenge: natively supported, but the level is not met.
        ItemStackSpec cobble = mockItemSpec();
        ChallengeExecutor executor = executor(challenge(
            List.of(),
            List.of(
                new InventoryItemsRequirement(List.of(new ItemRequirementSpec(cobble, 1, ItemAmountProgression.none()))),
                new IslandLevelRequirement(10)
            ),
            true, true));
        progressCache.replaceLoaded(ISLAND, new HashMap<>());
        ItemStack inInventory = mock(ItemStack.class);
        when(inInventory.isSimilar(any())).thenReturn(true);
        when(inInventory.getAmount()).thenReturn(5);
        when(playerInventory.getContents()).thenReturn(new ItemStack[]{inInventory});

        executor.attempt(player, CHALLENGE_ID);
        drainScheduledTasks();

        // Level requirement failed: nothing persists, and crucially no items were consumed.
        verify(repository, never()).replace(any(), any());
        verify(inInventory, never()).setAmount(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    public void refundsConsumedItemsWhenPersistFails() {
        ItemStackSpec cobble = mockItemSpec();
        ChallengeExecutor executor = executor(challenge(
            List.of(),
            List.of(new InventoryItemsRequirement(List.of(new ItemRequirementSpec(cobble, 2, ItemAmountProgression.none())))),
            true, true));
        progressCache.replaceLoaded(ISLAND, new HashMap<>());
        doThrow(new IllegalStateException("database unavailable")).when(repository).replace(any(), any());

        ItemStack inInventory = mock(ItemStack.class);
        when(inInventory.isSimilar(any())).thenReturn(true);
        when(inInventory.getAmount()).thenReturn(5);
        Inventory source = mock(Inventory.class);
        when(source.getContents()).thenReturn(new ItemStack[]{inInventory});

        executor.attempt(player, CHALLENGE_ID, List.of(source));
        drainScheduledTasks();

        verify(inInventory).setAmount(3);
        verify(playerInventory).addItem(any(ItemStack[].class));
        verify(rewardApplier, never()).apply(any(), any(), any(), anyBoolean());
        assertTrue(progressCache.getIfLoaded(ISLAND).orElseThrow().isWriteLocked());
    }

    @Test
    public void rejectsAttemptWhileCompletionInFlight() {
        ChallengeExecutor executor = executor(simpleChallenge());
        progressCache.replaceLoaded(ISLAND, new HashMap<>());
        assertTrue(progressCache.getIfLoaded(ISLAND).orElseThrow().tryBeginCompletion());

        executor.attempt(player, CHALLENGE_ID);
        drainScheduledTasks();

        verify(repository, never()).replace(any(), any());
    }

    @Test
    public void rejectsAttemptWhileWriteLocked() {
        ChallengeExecutor executor = executor(simpleChallenge());
        progressCache.replaceLoaded(ISLAND, new HashMap<>());
        progressCache.getIfLoaded(ISLAND).orElseThrow().lockWrites();

        executor.attempt(player, CHALLENGE_ID);
        drainScheduledTasks();

        verify(repository, never()).replace(any(), any());
    }

    @Test
    public void adminCompletePersistsThroughTheIslandLock() {
        ChallengeExecutor executor = executor(simpleChallenge());
        progressCache.replaceLoaded(ISLAND, new HashMap<>());
        Runnable onSuccess = mock(Runnable.class);

        executor.adminComplete(playerInfo, CHALLENGE_ID, onSuccess, error -> {
            throw new AssertionError(error);
        });
        drainScheduledTasks();

        verify(repository).replace(eq(ISLAND), any());
        verify(onSuccess).run();
        LoadedChallengeProgress loaded = progressCache.getIfLoaded(ISLAND).orElseThrow();
        assertEquals(1, loaded.snapshot().get(CHALLENGE_ID).getTimesCompleted());
        assertFalse(loaded.isCompletionInFlight());
    }

    @Test
    public void adminCompleteAllBatchesIntoOneLockAndPersist() {
        ChallengeExecutor executor = executor(simpleChallenge());
        ChallengeKey secondId = ChallengeKey.of("secondchallenge");
        // Build before stubbing: construction touches the mocked server's item factory.
        Optional<ChallengeDefinition> second = Optional.of(simpleChallenge());
        when(challengeLogic.getDefinitionById(secondId)).thenReturn(second);
        progressCache.replaceLoaded(ISLAND, new HashMap<>());
        Runnable onSuccess = mock(Runnable.class);

        // A per-challenge loop would deadlock on the island's in-flight lock within one tick.
        executor.adminCompleteAll(playerInfo, List.of(CHALLENGE_ID, secondId), onSuccess, error -> {
            throw new AssertionError(error);
        });
        drainScheduledTasks();

        verify(repository, org.mockito.Mockito.times(1)).replace(eq(ISLAND), any());
        verify(onSuccess).run();
        LoadedChallengeProgress loaded = progressCache.getIfLoaded(ISLAND).orElseThrow();
        assertEquals(1, loaded.snapshot().get(CHALLENGE_ID).getTimesCompleted());
        assertEquals(1, loaded.snapshot().get(secondId).getTimesCompleted());
    }

    @Test
    public void adminResetReportsErrorAndLocksWhenPersistFails() {
        ChallengeExecutor executor = executor(simpleChallenge());
        Map<ChallengeKey, ChallengeCompletion> existing = new HashMap<>();
        existing.put(CHALLENGE_ID, new ChallengeCompletion(CHALLENGE_ID, null, 3, 1));
        progressCache.replaceLoaded(ISLAND, existing);
        doThrow(new IllegalStateException("database unavailable")).when(repository).replace(any(), any());
        @SuppressWarnings("unchecked")
        java.util.function.Consumer<Throwable> onError = mock(java.util.function.Consumer.class);

        executor.adminReset(playerInfo, CHALLENGE_ID, () -> {
            throw new AssertionError("must not succeed");
        }, onError);
        drainScheduledTasks();

        verify(onError).accept(any());
        LoadedChallengeProgress loaded = progressCache.getIfLoaded(ISLAND).orElseThrow();
        assertEquals(3, loaded.snapshot().get(CHALLENGE_ID).getTimesCompleted());
        assertTrue(loaded.isWriteLocked());
    }

    @Test
    public void adminMutationReportsErrorWhenLoadFails() {
        ChallengeExecutor executor = executor(simpleChallenge());
        // No loaded entry: the mutation must load first, and a failing load must abort the
        // write path rather than write a near-default map over the island's stored progress.
        when(repository.load(any())).thenThrow(new IllegalStateException("database unavailable"));
        @SuppressWarnings("unchecked")
        java.util.function.Consumer<Throwable> onError = mock(java.util.function.Consumer.class);

        executor.adminComplete(playerInfo, CHALLENGE_ID, () -> {
            throw new AssertionError("must not succeed");
        }, onError);
        drainScheduledTasks();

        verify(onError).accept(any());
        verify(repository, never()).replace(any(), any());
    }

    @Test
    public void adminResetAllRestoresDefaultProgress() {
        ChallengeExecutor executor = executor(simpleChallenge());
        Map<ChallengeKey, ChallengeCompletion> existing = new HashMap<>();
        existing.put(CHALLENGE_ID, new ChallengeCompletion(CHALLENGE_ID, null, 3, 1));
        progressCache.replaceLoaded(ISLAND, existing);
        org.mockito.Mockito.doAnswer(invocation -> {
            Map<ChallengeKey, ChallengeCompletion> map = invocation.getArgument(0);
            map.put(CHALLENGE_ID, new ChallengeCompletion(CHALLENGE_ID, null, 0, 0));
            return null;
        }).when(challengeLogic).populateChallenges(any());

        executor.adminResetAll(playerInfo, () -> {
        }, error -> {
            throw new AssertionError(error);
        });
        drainScheduledTasks();

        assertEquals(0, progressCache.getIfLoaded(ISLAND).orElseThrow().snapshot().get(CHALLENGE_ID).getTimesCompleted());
    }

    private ItemStackSpec mockItemSpec() {
        ItemStackSpec spec = mock(ItemStackSpec.class);
        ItemStack required = mock(ItemStack.class);
        ItemStack refunded = mock(ItemStack.class);
        when(spec.create()).thenReturn(required);
        when(required.clone()).thenReturn(refunded);
        when(required.getMaxStackSize()).thenReturn(64);
        when(refunded.getMaxStackSize()).thenReturn(64);
        when(refunded.getAmount()).thenReturn(2);
        return spec;
    }

    private void drainScheduledTasks() {
        while (!scheduledTasks.isEmpty()) {
            scheduledTasks.poll().run();
        }
    }

    private Scheduler deferringScheduler() {
        Scheduler scheduler = mock(Scheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(scheduler.async(any(Runnable.class))).thenAnswer(invocation -> {
            scheduledTasks.add(invocation.getArgument(0));
            return task;
        });
        when(scheduler.sync(any(Runnable.class))).thenAnswer(invocation -> {
            scheduledTasks.add(invocation.getArgument(0));
            return task;
        });
        when(scheduler.async(any(Runnable.class), any(Duration.class), any(Duration.class))).thenReturn(task);
        return scheduler;
    }
}
