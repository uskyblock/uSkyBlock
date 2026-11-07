package us.talabrek.ultimateskyblock.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.test.MutableClock;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GuiManagerTest {
    private final MutableClock clock = new MutableClock(Instant.EPOCH);
    private final GuiManager guiManager = new GuiManager(new DoubleClickGuard(clock));
    private final Inventory menu = mock(Inventory.class);
    private final Player player = mock(Player.class);
    private final Inventory guiInventory = mock(Inventory.class);
    private final List<Integer> pressedSlots = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        when(menu.getSize()).thenReturn(27);
        guiManager.registerHandledInventory(menu, mock(InventoryHandler.class));
        when(player.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        InventoryGui gui = new InventoryGui(guiInventory) {
        };
        gui.addButton(3, new InventoryButton().consumer((player, event) -> pressedSlots.add(3)));
        gui.addButton(4, new InventoryButton().consumer((player, event) -> pressedSlots.add(4)));
        gui.addButton(5, new InventoryButton().repeatable().consumer((player, event) -> pressedSlots.add(5)));
        guiManager.registerHandledInventory(guiInventory, gui);
    }

    @Test
    public void doubleClickOnInventoryGuiButtonRunsOnlyTheFirstPress() {
        doubleClick(4);

        assertThat(pressedSlots, contains(4));
    }

    @Test
    public void doubleClickOnRepeatableInventoryGuiButtonRunsBothPresses() {
        doubleClick(5);

        assertThat(pressedSlots, contains(5, 5));
    }

    @Test
    public void secondClickOnInventoryGuiButtonAfterTheDoubleClickWindowRuns() {
        guiManager.handleClick(guiClick(4, ClickType.LEFT));
        clock.advance(Duration.ofMillis(300));
        guiManager.handleClick(guiClick(4, ClickType.LEFT));

        assertThat(pressedSlots, contains(4, 4));
    }

    @Test
    public void quickClickOnAnotherInventoryGuiButtonRuns() {
        guiManager.handleClick(guiClick(4, ClickType.LEFT));
        clock.advance(Duration.ofMillis(100));
        guiManager.handleClick(guiClick(3, ClickType.LEFT));

        assertThat(pressedSlots, contains(4, 3));
    }

    @Test
    public void dragIntoMenuIsCancelled() {
        InventoryDragEvent event = dragEvent(menu, Set.of(40, 26));

        guiManager.handleDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void dragWithinPlayerInventoryIsAllowed() {
        InventoryDragEvent event = dragEvent(menu, Set.of(27, 50));

        guiManager.handleDrag(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    public void dragIntoUnhandledInventoryIsAllowed() {
        Inventory other = mock(Inventory.class);
        when(other.getSize()).thenReturn(27);
        InventoryDragEvent event = dragEvent(other, Set.of(0));

        guiManager.handleDrag(event);

        verify(event, never()).setCancelled(true);
    }

    private void doubleClick(int slot) {
        guiManager.handleClick(guiClick(slot, ClickType.LEFT));
        clock.advance(Duration.ofMillis(150));
        guiManager.handleClick(guiClick(slot, ClickType.LEFT));
        clock.advance(Duration.ofMillis(50));
        guiManager.handleClick(guiClick(slot, ClickType.DOUBLE_CLICK));
    }

    private InventoryClickEvent guiClick(int slot, ClickType clickType) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(guiInventory);
        when(event.getClickedInventory()).thenReturn(guiInventory);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getClick()).thenReturn(clickType);
        when(event.getSlot()).thenReturn(slot);
        when(event.getRawSlot()).thenReturn(slot);
        return event;
    }

    private static InventoryDragEvent dragEvent(Inventory top, Set<Integer> rawSlots) {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getInventory()).thenReturn(top);
        when(event.getRawSlots()).thenReturn(rawSlots);
        return event;
    }
}
