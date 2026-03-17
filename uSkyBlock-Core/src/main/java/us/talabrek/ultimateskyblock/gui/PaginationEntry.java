package us.talabrek.ultimateskyblock.gui;

public record PaginationEntry(int slotIndex, int pageNumber, Kind kind, boolean current) {
    public PaginationEntry {
        if (slotIndex < 0 || slotIndex > 8) {
            throw new IllegalArgumentException("slotIndex must be between 0 and 8");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
    }

    public enum Kind {
        FIRST,
        PAGE,
        LAST
    }
}
