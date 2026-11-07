package us.talabrek.ultimateskyblock.gui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * The client sends a fast double click as LEFT, LEFT, DOUBLE_CLICK. Menus act on the first press only,
 * so a double click cannot arm and confirm a "click again to confirm" button in one go.
 */
@Singleton
public class DoubleClickGuard {
    static final Duration WINDOW = Duration.ofMillis(250);

    private final Clock clock;
    private final Map<UUID, Press> lastPresses = new HashMap<>();

    private record Press(int rawSlot, @NotNull Instant time) {
    }

    @Inject
    public DoubleClickGuard(@NotNull Clock clock) {
        this.clock = requireNonNull(clock);
    }

    /**
     * Cancels the click and returns true when it is the rest of a double click on the same slot.
     */
    public boolean consumeRepeat(@NotNull InventoryClickEvent event) {
        if (consumeDoubleClick(event)) {
            return true;
        }
        Instant now = clock.instant();
        lastPresses.values().removeIf(press -> !isWithinWindow(press, now));
        UUID playerId = event.getWhoClicked().getUniqueId();
        Press last = lastPresses.get(playerId);
        if (last != null && last.rawSlot() == event.getRawSlot()) {
            event.setCancelled(true);
            return true;
        }
        lastPresses.put(playerId, new Press(event.getRawSlot(), now));
        return false;
    }

    /**
     * Cancels the click and returns true when it is the DOUBLE_CLICK that ends a double click, for buttons
     * where both presses are meant to act.
     */
    public boolean consumeDoubleClick(@NotNull InventoryClickEvent event) {
        requireNonNull(event);
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    private static boolean isWithinWindow(@NotNull Press press, @NotNull Instant now) {
        return Duration.between(press.time(), now).compareTo(WINDOW) < 0;
    }
}
