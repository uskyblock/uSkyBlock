package us.talabrek.ultimateskyblock.gui;

import java.util.ArrayList;
import java.util.List;

public final class PaginationBars {
    private PaginationBars() {
    }

    public static PaginationBar nineSlotBar(int currentPage, int totalPages) {
        if (currentPage < 1 || currentPage > totalPages) {
            throw new IllegalArgumentException("currentPage must be between 1 and totalPages");
        }
        List<PaginationEntry> entries = new ArrayList<>();
        entries.add(new PaginationEntry(0, 1, PaginationEntry.Kind.FIRST, currentPage == 1));
        entries.addAll(windowEntries(currentPage, totalPages));
        entries.add(new PaginationEntry(8, totalPages, PaginationEntry.Kind.LAST, currentPage == totalPages));
        return new PaginationBar(currentPage, totalPages, entries);
    }

    private static List<PaginationEntry> windowEntries(int currentPage, int totalPages) {
        List<PaginationEntry> entries = new ArrayList<>();
        int startPage = 2;
        if (currentPage > 5) {
            startPage = Math.max(2, (int) Math.round(currentPage / 2d) - 1);
            startPage = Math.min(startPage, Math.max(2, totalPages - 7));
        }
        for (int i = 0; i < 7; i++) {
            int page = startPage + i;
            if (page >= 1 && page <= totalPages) {
                entries.add(new PaginationEntry(i + 1, page, PaginationEntry.Kind.PAGE, page == currentPage));
            }
        }
        return entries;
    }
}
