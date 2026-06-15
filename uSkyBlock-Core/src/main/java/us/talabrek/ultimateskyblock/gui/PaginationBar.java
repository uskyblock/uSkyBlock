package us.talabrek.ultimateskyblock.gui;

import java.util.List;
import java.util.Objects;

public record PaginationBar(int currentPage, int totalPages, List<PaginationEntry> entries) {
    public PaginationBar {
        if (currentPage < 1) {
            throw new IllegalArgumentException("currentPage must be positive");
        }
        if (totalPages < 1) {
            throw new IllegalArgumentException("totalPages must be positive");
        }
        if (currentPage > totalPages) {
            throw new IllegalArgumentException("currentPage cannot exceed totalPages");
        }
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }
}
