package us.talabrek.ultimateskyblock.gui;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationBarsTest {
    @Test
    void buildsNineSlotBarAroundCurrentPage() {
        PaginationBar paginationBar = PaginationBars.nineSlotBar(6, 10);
        Map<Integer, PaginationEntry> entriesBySlot = paginationBar.entries().stream()
            .collect(Collectors.toMap(PaginationEntry::slotIndex, entry -> entry));

        assertEquals(6, paginationBar.currentPage());
        assertEquals(10, paginationBar.totalPages());
        assertEquals(1, entriesBySlot.get(0).pageNumber());
        assertEquals(10, entriesBySlot.get(8).pageNumber());
        assertEquals(2, entriesBySlot.get(1).pageNumber());
        assertEquals(8, entriesBySlot.get(7).pageNumber());
        assertTrue(entriesBySlot.get(5).current());
    }

    @Test
    void rejectsOutOfRangeCurrentPage() {
        assertThrows(IllegalArgumentException.class, () -> PaginationBars.nineSlotBar(0, 1));
        assertThrows(IllegalArgumentException.class, () -> PaginationBars.nineSlotBar(2, 1));
    }
}
