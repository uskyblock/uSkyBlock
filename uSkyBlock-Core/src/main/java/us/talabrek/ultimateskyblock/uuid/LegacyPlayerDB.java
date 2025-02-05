package us.talabrek.ultimateskyblock.uuid;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import us.talabrek.ultimateskyblock.api.model.Player;
import us.talabrek.ultimateskyblock.storage.SkyStorage;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Set;
import java.util.UUID;

/**
 * Legacy {@link PlayerDB} implementation that queries the new SQL-based
 * {@link SkyStorage} system.
 */
public class LegacyPlayerDB implements PlayerDB {
    private final uSkyBlock plugin;

    public LegacyPlayerDB(uSkyBlock plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void shutdown() {
        // ignore, handled by the upstream Storage implementation.
    }

    @Override
    public OfflinePlayer getOfflinePlayer(UUID uuid) {
        return plugin.getServer().getOfflinePlayer(uuid);
    }

    @Override
    public org.bukkit.entity.Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }

    @Override
    public org.bukkit.entity.Player getPlayer(UUID uuid) {
        return plugin.getServer().getPlayer(uuid);
    }

    @Override
    public void updatePlayer(UUID uuid, String name, String displayName) {
        Player player = new Player(uuid, name, displayName);
        plugin.getStorage().savePlayer(player);
    }

    @Override
    public Set<String> getNames(String search) {
        throw new UnsupportedOperationException("Not implemented, deprecated!");
    }

    @Override
    public String getDisplayName(String playerName) {
        Player playerInfo = plugin.getStorage().getPlayer(playerName).join();
        return playerInfo != null ? playerInfo.getDisplayName() : null;
    }

    @Override
    public String getDisplayName(UUID uuid) {
        Player playerInfo = plugin.getStorage().getPlayer(uuid).join();
        return playerInfo != null ? playerInfo.getName() : null;
    }

    @Override
    public String getName(UUID uuid) {
        // Lookup, but cache in our database afterwards.
        if (UNKNOWN_PLAYER_UUID.equals(uuid)) {
            return UNKNOWN_PLAYER_NAME;
        }

        Player playerInfo = plugin.getStorage().getPlayer(uuid).join();

        if (playerInfo == null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.hasPlayedBefore()) {
                updatePlayer(offlinePlayer.getUniqueId(), offlinePlayer.getName(), offlinePlayer.getName());
                return offlinePlayer.getName();
            }
        }
        return playerInfo != null ? playerInfo.getName() : null;
    }

    @Override
    public UUID getUUIDFromName(String name, boolean lookup) {
        if (UNKNOWN_PLAYER_NAME.equalsIgnoreCase(name)) {
            return UNKNOWN_PLAYER_UUID;
        }

        Player playerInfo = plugin.getStorage().getPlayer(name).join();
        return playerInfo != null ? playerInfo.getUuid() : Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    @Override
    public UUID getUUIDFromName(String name) {
        return getUUIDFromName(name, false);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName(), event.getPlayer().getDisplayName());
    }
}
