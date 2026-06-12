package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.island.IslandKey;
import us.talabrek.ultimateskyblock.player.PerkLogic;
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
import static org.mockito.ArgumentMatchers.anyInt;
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
    private final uSkyBlock plugin = mock(uSkyBlock.class);
    private final Player player = mock(Player.class);
    private final PlayerInfo playerInfo = mock(PlayerInfo.class);
    private final PlayerInventory playerInventory = mock(PlayerInventory.class);
    private final Challenge challenge = mock(Challenge.class);
    private final Rank rank = mock(Rank.class);
    private final Reward reward = new Reward("", List.of(), null, 0, 0, List.of());

    private ChallengeProgressCache progressCache;
    private ChallengeExecutor executor;
    /**
     * Tasks handed to the mocked scheduler; drained after each executor call. Deferring (instead
     * of running inline) mirrors the real scheduler and avoids re-entrant cache updates.
     */
    private final Deque<Runnable> scheduledTasks = new ArrayDeque<>();

    @BeforeEach
    void setUp() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Scheduler scheduler = deferringScheduler();
        progressCache = new ChallengeProgressCache(Logger.getAnonymousLogger(), scheduler, repository, challengeLogic);
        executor = new ChallengeExecutor(Logger.getAnonymousLogger(), plugin, scheduler, challengeLogic,
            defaults(), mock(HookManager.class), mock(PerkLogic.class), progressCache, repository);

        when(plugin.getPlayerInfo(player)).thenReturn(playerInfo);
        when(plugin.playerIsOnOwnIsland(player)).thenReturn(true);
        when(playerInfo.getHasIsland()).thenReturn(true);
        when(playerInfo.locationForParty()).thenReturn("1,1");
        when(playerInfo.getIslandInfo()).thenReturn(null);
        when(player.getName()).thenReturn("Tester");
        when(player.isOnline()).thenReturn(true);
        when(player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
        when(playerInventory.getContents()).thenReturn(new ItemStack[0]);

        when(challengeLogic.getChallengeById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(challenge.getId()).thenReturn(CHALLENGE_ID);
        when(challenge.getType()).thenReturn(Challenge.Type.PLAYER);
        when(challenge.getRank()).thenReturn(rank);
        when(challenge.isRepeatable()).thenReturn(true);
        when(challenge.getRepeatLimit()).thenReturn(0);
        when(challenge.getResetDuration()).thenReturn(Duration.ZERO);
        when(challenge.getRequiredItems(anyInt())).thenReturn(Map.of());
        when(challenge.isTakeItems()).thenReturn(false);
        when(challenge.getReward()).thenReturn(reward);
        when(challenge.getRepeatReward()).thenReturn(reward);
        when(challenge.getDisplayName()).thenReturn("Test Challenge");
        when(challenge.getName()).thenReturn("testchallenge");
        when(rank.isAvailable(playerInfo)).thenReturn(true);
    }

    @Test
    public void appliesRewardOnlyAfterSuccessfulPersist() {
        progressCache.replaceLoaded(ISLAND, new HashMap<>());

        executor.attempt(player, CHALLENGE_ID);
        drainScheduledTasks();

        InOrder order = inOrder(repository, player);
        order.verify(repository).replace(eq(ISLAND), any());
        order.verify(player).giveExp(0);
        LoadedChallengeProgress loaded = progressCache.getIfLoaded(ISLAND).orElseThrow();
        assertEquals(1, loaded.snapshot().get(CHALLENGE_ID).getTimesCompleted());
        assertFalse(loaded.isCompletionInFlight());
    }

    @Test
    public void withholdsRewardAndKeepsMemoryWhenPersistFails() {
        progressCache.replaceLoaded(ISLAND, new HashMap<>());
        doThrow(new IllegalStateException("database unavailable")).when(repository).replace(any(), any());

        executor.attempt(player, CHALLENGE_ID);
        drainScheduledTasks();

        verify(player, never()).giveExp(anyInt());
        LoadedChallengeProgress loaded = progressCache.getIfLoaded(ISLAND).orElseThrow();
        // In-memory state must still match the database: the completion never happened.
        assertFalse(loaded.snapshot().containsKey(CHALLENGE_ID));
        assertTrue(loaded.isWriteLocked());
    }

    @Test
    public void refundsConsumedItemsWhenPersistFails() {
        progressCache.replaceLoaded(ISLAND, new HashMap<>());
        doThrow(new IllegalStateException("database unavailable")).when(repository).replace(any(), any());

        ItemStack required = mock(ItemStack.class);
        ItemStack refunded = mock(ItemStack.class);
        when(required.clone()).thenReturn(refunded);
        when(required.getMaxStackSize()).thenReturn(64);
        when(refunded.getMaxStackSize()).thenReturn(64);
        when(refunded.getAmount()).thenReturn(2);
        ItemStack inInventory = mock(ItemStack.class);
        when(inInventory.isSimilar(required)).thenReturn(true);
        when(inInventory.getAmount()).thenReturn(5);
        Inventory source = mock(Inventory.class);
        when(source.getContents()).thenReturn(new ItemStack[]{inInventory});

        when(challenge.getRequiredItems(anyInt())).thenReturn(Map.of(required, 2));
        when(challenge.isTakeItems()).thenReturn(true);

        executor.attempt(player, CHALLENGE_ID, List.of(source));
        drainScheduledTasks();

        verify(inInventory).setAmount(3);
        verify(playerInventory).addItem(refunded);
        verify(player, never()).giveExp(anyInt());
        assertTrue(progressCache.getIfLoaded(ISLAND).orElseThrow().isWriteLocked());
    }

    @Test
    public void rejectsAttemptWhileCompletionInFlight() {
        progressCache.replaceLoaded(ISLAND, new HashMap<>());
        assertTrue(progressCache.getIfLoaded(ISLAND).orElseThrow().tryBeginCompletion());

        executor.attempt(player, CHALLENGE_ID);
        drainScheduledTasks();

        verify(repository, never()).replace(any(), any());
    }

    @Test
    public void rejectsAttemptWhileWriteLocked() {
        progressCache.replaceLoaded(ISLAND, new HashMap<>());
        progressCache.getIfLoaded(ISLAND).orElseThrow().lockWrites();

        executor.attempt(player, CHALLENGE_ID);
        drainScheduledTasks();

        verify(repository, never()).replace(any(), any());
    }

    @Test
    public void adminCompletePersistsThroughTheIslandLock() {
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
    public void adminResetReportsErrorAndLocksWhenPersistFails() {
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

    private static ChallengeDefaults defaults() {
        return new ChallengeDefaults(Duration.ofHours(144), false, "", "", "", 0, false, false, 10, false, 0);
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
