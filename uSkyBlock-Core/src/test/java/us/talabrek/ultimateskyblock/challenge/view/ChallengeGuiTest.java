package us.talabrek.ultimateskyblock.challenge.view;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeProperties;
import us.talabrek.ultimateskyblock.challenge.catalog.DisplaySpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDisplaySpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;
import us.talabrek.ultimateskyblock.challenge.catalog.RepeatPolicy;
import us.talabrek.ultimateskyblock.challenge.catalog.RewardBundle;
import us.talabrek.ultimateskyblock.challenge.catalog.TextSpec;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChallengeGuiTest {
    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        Server server = BukkitServerMock.setupServerMock();
        when(server.createInventory(any(), anyInt(), anyString())).thenAnswer(invocation -> inventoryMock(
            invocation.getArgument(1, Integer.class),
            invocation.getArgument(2, String.class)
        ));
    }

    @Test
    void decoratesChallengeAndPaginationSlots() {
        ChallengePageView pageView = new ChallengeMenuViewAssembler().assemblePage(
            new ChallengeCatalog(List.of(
                rank("starter", Material.BARRIER, List.of(
                    challenge("alpha", Material.STONE, Material.OBSIDIAN),
                    challenge("beta", Material.COBBLESTONE, Material.GOLD_BLOCK)
                )),
                rank("adept", Material.BARRIER, List.of(
                    challenge("gamma", Material.IRON_INGOT, Material.DIAMOND_BLOCK)
                ))
            )),
            new ChallengePresentationSnapshot(Set.of(RankId.of("starter"), RankId.of("adept")), Set.of(ChallengeId.of("alpha"))),
            1
        );
        ChallengeGui gui = new ChallengeGui(pageView, page -> {}, (player, slot) -> {});

        Player player = mock(Player.class);
        gui.decorate(player);

        Inventory inventory = gui.getInventory();
        assertEquals(Material.STONE, inventory.getItem(0).getType());
        assertEquals(Material.GOLD_BLOCK, inventory.getItem(1).getType());
        assertEquals(Material.DIAMOND_BLOCK, inventory.getItem(9).getType());
        assertEquals(Material.WRITABLE_BOOK, inventory.getItem(45).getType());
        assertEquals(Material.WRITABLE_BOOK, inventory.getItem(53).getType());
    }

    @Test
    void routesClicksToChallengeAndPaginationCallbacks() {
        ChallengePageView pageView = new ChallengeMenuViewAssembler().assemblePage(
            new ChallengeCatalog(List.of(
                rank("starter", Material.BARRIER, List.of(challenge("alpha", Material.STONE, Material.OBSIDIAN))),
                rank("adept", Material.BARRIER, List.of(challenge("beta", Material.COBBLESTONE, Material.GOLD_BLOCK))),
                rank("expert", Material.BARRIER, List.of(challenge("gamma", Material.IRON_INGOT, Material.DIAMOND_BLOCK))),
                rank("master", Material.BARRIER, List.of(challenge("delta", Material.GOLD_INGOT, Material.EMERALD_BLOCK))),
                rank("legend", Material.BARRIER, List.of(challenge("epsilon", Material.REDSTONE, Material.LAPIS_BLOCK))),
                rank("mythic", Material.BARRIER, List.of(challenge("zeta", Material.COAL, Material.NETHERITE_BLOCK)))
            )),
            new ChallengePresentationSnapshot(
                Set.of(RankId.of("starter"), RankId.of("adept"), RankId.of("expert"), RankId.of("master"), RankId.of("legend"), RankId.of("mythic")),
                Set.of(ChallengeId.of("alpha"), ChallengeId.of("beta"), ChallengeId.of("gamma"), ChallengeId.of("delta"), ChallengeId.of("epsilon"), ChallengeId.of("zeta"))
            ),
            1
        );

        AtomicInteger clickedPage = new AtomicInteger(-1);
        AtomicReference<ChallengeId> clickedChallenge = new AtomicReference<>();
        ChallengeGui gui = new ChallengeGui(pageView, clickedPage::set, (player, slot) -> clickedChallenge.set(slot.challenge().id()));
        Player player = mock(Player.class);
        gui.decorate(player);

        gui.onClick(clickEvent(gui.getInventory(), player, 0));
        gui.onClick(clickEvent(gui.getInventory(), player, 53));

        assertEquals(ChallengeId.of("alpha"), clickedChallenge.get());
        assertEquals(2, clickedPage.get());
    }

    @Test
    void lockedSlotsDoNotInvokeChallengeCallback() {
        ChallengePageView pageView = new ChallengeMenuViewAssembler().assemblePage(
            new ChallengeCatalog(List.of(rank("starter", Material.BARRIER, List.of(
                challenge("alpha", Material.STONE, Material.OBSIDIAN)
            )))),
            new ChallengePresentationSnapshot(Set.of(), Set.of()),
            1
        );

        AtomicReference<ChallengeId> clickedChallenge = new AtomicReference<>();
        ChallengeGui gui = new ChallengeGui(pageView, page -> {}, (player, slot) -> clickedChallenge.set(slot.challenge().id()));
        Player player = mock(Player.class);
        gui.decorate(player);

        gui.onClick(clickEvent(gui.getInventory(), player, 0));

        assertNull(clickedChallenge.get());
    }

    private static RankDefinition rank(String id, Material lockedItem, List<ChallengeDefinition> challenges) {
        return new RankDefinition(
            RankId.of(id),
            new RankDisplaySpec(TextSpec.miniMessage(id), TextSpec.empty()),
            item(lockedItem),
            List.of(),
            challenges
        );
    }

    private static ChallengeDefinition challenge(String id, Material displayItem, Material lockedDisplayItem) {
        return new ChallengeDefinition(
            ChallengeId.of(id),
            new DisplaySpec(TextSpec.miniMessage(id), TextSpec.empty(), item(displayItem)),
            item(lockedDisplayItem),
            List.of(),
            List.of(),
            new ChallengeProperties(true),
            new RepeatPolicy(false, Duration.ZERO, 0),
            RewardBundle.empty(),
            RewardBundle.empty()
        );
    }

    private static ItemStackSpec item(Material material) {
        return new ItemStackSpec(new ItemStack(material));
    }

    private static InventoryClickEvent clickEvent(Inventory inventory, Player player, int slot) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getClickedInventory()).thenReturn(inventory);
        when(event.getSlot()).thenReturn(slot);
        when(event.getClick()).thenReturn(ClickType.LEFT);
        when(event.getWhoClicked()).thenReturn(player);
        return event;
    }

    private static Inventory inventoryMock(int size, String title) {
        Inventory inventory = mock(Inventory.class);
        Map<Integer, ItemStack> items = new HashMap<>();
        when(inventory.getSize()).thenReturn(size);
        when(inventory.getItem(anyInt())).thenAnswer(invocation -> items.get(invocation.getArgument(0, Integer.class)));
        when(inventory.getContents()).thenAnswer(invocation -> {
            ItemStack[] contents = new ItemStack[size];
            items.forEach((slot, item) -> contents[slot] = item);
            return contents;
        });
        when(inventory.getMaxStackSize()).thenReturn(64);
        when(inventory.getStorageContents()).thenAnswer(invocation -> inventory.getContents());
        when(inventory.getViewers()).thenReturn(List.of());
        when(inventory.getLocation()).thenReturn(null);
        when(inventory.getHolder()).thenReturn(null);
        when(inventory.isEmpty()).thenAnswer(invocation -> items.isEmpty());
        when(inventory.getItem(anyInt())).thenAnswer(invocation -> items.get(invocation.getArgument(0, Integer.class)));
        org.mockito.Mockito.doAnswer(invocation -> {
            items.put(invocation.getArgument(0, Integer.class), invocation.getArgument(1, ItemStack.class));
            return null;
        }).when(inventory).setItem(anyInt(), any(ItemStack.class));
        return inventory;
    }
}
