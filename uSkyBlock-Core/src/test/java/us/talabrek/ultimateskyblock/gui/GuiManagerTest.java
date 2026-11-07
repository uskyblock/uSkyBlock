package us.talabrek.ultimateskyblock.gui;

import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GuiManagerTest {
    private final GuiManager guiManager = new GuiManager();
    private final Inventory menu = mock(Inventory.class);

    @BeforeEach
    public void setUp() {
        when(menu.getSize()).thenReturn(27);
        guiManager.registerHandledInventory(menu, mock(InventoryHandler.class));
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

    private static InventoryDragEvent dragEvent(Inventory top, Set<Integer> rawSlots) {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getInventory()).thenReturn(top);
        when(event.getRawSlots()).thenReturn(rawSlots);
        return event;
    }
}
