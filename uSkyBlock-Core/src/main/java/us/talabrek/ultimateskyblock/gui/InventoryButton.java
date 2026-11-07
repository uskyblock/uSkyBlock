package us.talabrek.ultimateskyblock.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class InventoryButton {

    private Function<Player, ItemStack> iconCreator;
    private BiConsumer<Player, InventoryClickEvent> eventConsumer = (player, event) -> {
    };
    private boolean repeatable;

    public @NotNull InventoryButton creator(@NotNull Function<Player, ItemStack> iconCreator) {
        this.iconCreator = iconCreator;
        return this;
    }

    public @NotNull InventoryButton consumer(@NotNull BiConsumer<Player, InventoryClickEvent> eventConsumer) {
        this.eventConsumer = eventConsumer;
        return this;
    }

    /**
     * Lets a quick second press act again, for step buttons like +/-.
     */
    public @NotNull InventoryButton repeatable() {
        this.repeatable = true;
        return this;
    }

    public boolean isRepeatable() {
        return this.repeatable;
    }

    public @NotNull BiConsumer<Player, InventoryClickEvent> getEventConsumer() {
        requireNonNull(this.eventConsumer, "EventConsumer is not set");
        return this.eventConsumer;
    }

    public @NotNull Function<Player, ItemStack> getIconCreator() {
        requireNonNull(this.iconCreator, "IconCreator is not set");
        return this.iconCreator;
    }
}
