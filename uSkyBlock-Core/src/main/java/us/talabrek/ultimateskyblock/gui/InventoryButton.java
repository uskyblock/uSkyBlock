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

    public @NotNull InventoryButton creator(@NotNull Function<Player, ItemStack> iconCreator) {
        this.iconCreator = iconCreator;
        return this;
    }

    public @NotNull InventoryButton consumer(@NotNull BiConsumer<Player, InventoryClickEvent> eventConsumer) {
        this.eventConsumer = eventConsumer;
        return this;
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
