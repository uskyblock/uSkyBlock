package us.talabrek.ultimateskyblock.player;

import com.google.inject.Inject;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class NotificationManager {
    private final BukkitAudiences audiences;

    @Inject
    public NotificationManager(Plugin plugin) {
        audiences = BukkitAudiences.create(plugin);
    }

    /**
     * Sends the given {@link Component} as message to the {@link Player}'s ActionBar.
     *
     * @param player    Player to send the given message to
     * @param component Component to send to the given player
     */
    public void sendActionBar(@NotNull Player player, @NotNull Component component) {
        audiences.player(player).sendActionBar(component);
    }

    public void sendMessage(@NotNull CommandSender sender, @NotNull Component component) {
        audiences.sender(sender).sendMessage(component);
    }

    public void shutdown() {
        audiences.close();
    }
}
