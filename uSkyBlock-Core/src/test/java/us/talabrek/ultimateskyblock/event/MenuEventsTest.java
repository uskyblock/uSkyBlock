package us.talabrek.ultimateskyblock.event;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.gui.DoubleClickGuard;
import us.talabrek.ultimateskyblock.menu.SkyBlockMenu;
import us.talabrek.ultimateskyblock.player.UltimateHolder;
import us.talabrek.ultimateskyblock.player.UltimateHolder.MenuType;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MenuEventsTest {
    private SkyBlockMenu fakeMenu;

    private MenuEvents menuEvents;

    @BeforeEach
    public void setUp() {
        fakeMenu = mock(SkyBlockMenu.class);

        doNothing().when(fakeMenu).onClick(any(InventoryClickEvent.class));

        menuEvents = new MenuEvents(fakeMenu, new DoubleClickGuard(Clock.systemUTC()));
    }

    @Test
    public void testOnGuiClick_regularMenu() {
        UltimateHolder holder = new UltimateHolder(getFakePlayer(), "Island GUI menu", MenuType.DEFAULT);
        InventoryClickEvent event = getEvent(holder);

        menuEvents.guiClick(event);
        verify(fakeMenu).onClick(event);
    }

    @Test
    public void testOnGuiClick_noUltimateHolder() {
        InventoryHolder holder = mock(InventoryHolder.class);
        InventoryClickEvent event = getEvent(holder);

        menuEvents.guiClick(event);
        verify(fakeMenu, times(0)).onClick(event);
    }

    @Test
    public void dragIntoMenuIsCancelled() {
        UltimateHolder holder = new UltimateHolder(getFakePlayer(), "Island GUI menu", MenuType.DEFAULT);
        InventoryDragEvent event = getDragEvent(holder, Set.of(20, 4));

        menuEvents.guiDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void dragWithinPlayerInventoryIsAllowed() {
        UltimateHolder holder = new UltimateHolder(getFakePlayer(), "Island GUI menu", MenuType.DEFAULT);
        InventoryDragEvent event = getDragEvent(holder, Set.of(18, 30));

        menuEvents.guiDrag(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    public void dragIntoOtherInventoryIsAllowed() {
        InventoryDragEvent event = getDragEvent(mock(InventoryHolder.class), Set.of(4));

        menuEvents.guiDrag(event);

        verify(event, never()).setCancelled(true);
    }

    private InventoryDragEvent getDragEvent(InventoryHolder holder, Set<Integer> rawSlots) {
        Inventory menu = mock(Inventory.class);
        when(menu.getHolder()).thenReturn(holder);
        when(menu.getSize()).thenReturn(18);
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getInventory()).thenReturn(menu);
        when(event.getRawSlots()).thenReturn(rawSlots);
        return event;
    }

    private Player getFakePlayer() {
        Player fakePlayer = mock(Player.class);
        when(fakePlayer.getUniqueId()).thenReturn(UUID.fromString("29292160-6d49-47a3-ae1c-7c800e14cca3"));
        when(fakePlayer.getName()).thenReturn("linksssofrechts");
        return fakePlayer;
    }

    private InventoryClickEvent getEvent(InventoryHolder holder) {
        // These values are not checked anywhere.
        InventoryView inventoryView = mock(InventoryView.class);
        InventoryType.SlotType slotType = InventoryType.SlotType.QUICKBAR;
        ClickType clickType = ClickType.LEFT;
        InventoryAction inventoryAction = InventoryAction.NOTHING;

        Inventory fakeInventory = mock(Inventory.class);
        doReturn(holder).when(fakeInventory).getHolder();
        doReturn(fakeInventory).when(inventoryView).getTopInventory();
        doReturn(getFakePlayer()).when(inventoryView).getPlayer();

        return new InventoryClickEvent(inventoryView, slotType, 1, clickType, inventoryAction);
    }
}
