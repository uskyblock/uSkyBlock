package us.talabrek.ultimateskyblock.player;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Holds the active players
 */
public class PlayerLogic {
    private final LoadingCache<UUID, us.talabrek.ultimateskyblock.api.model.Player> databasePlayerCache;
    private final uSkyBlock plugin;
    private final BukkitTask saveTask;
    private final PlayerDB playerDB;
    private final NotificationManager notificationManager;

    public PlayerLogic(uSkyBlock plugin) {
        this.plugin = plugin;
        playerDB = plugin.getPlayerDB();

        databasePlayerCache = CacheBuilder
            .from(plugin.getConfig().getString("options.advanced.playerCache", "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m"))
            .removalListener((RemovalListener<UUID, us.talabrek.ultimateskyblock.api.model.Player>) removal -> {
                plugin.getLog4JLogger().info("Unloading player {} from database cache.", removal.getKey());
                plugin.getStorage().savePlayer(removal.getValue());
            })
            .build(new CacheLoader<>() {
                @Override
                public @NotNull us.talabrek.ultimateskyblock.api.model.Player load(@NotNull UUID uuid) {
                    plugin.getLog4JLogger().info("Loading player {} to database cache.", uuid);

                    plugin.sync(() -> loadPlayerData(uuid), 100);
                    return plugin.getStorage().getPlayer(uuid).join();
                }
            });

        long every = TimeUtil.secondsAsMillis(plugin.getConfig().getInt("options.advanced.player.saveEvery", 2*60));
        saveTask = plugin.async(this::saveDirtyToFiles, every, every);
        notificationManager = new NotificationManager(plugin);
    }

    private void saveDirtyToFiles() {
        databasePlayerCache.asMap().values().forEach(player -> {
            if (player.isDirty()) plugin.getStorage().savePlayer(player);
        });
    }

    private void loadPlayerData(UUID uuid) {
        final Player onlinePlayer = plugin.getPlayerDB().getPlayer(uuid);
        final PlayerInfo playerInfo = new PlayerInfo(databasePlayerCache.getUnchecked(uuid), plugin);

        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            if (playerInfo.getHasIsland()) {
                IslandInfo islandInfo = plugin.getIslandInfo(playerInfo);
                if (islandInfo != null) {
                    islandInfo.updatePermissionPerks(onlinePlayer, plugin.getPerkLogic().getPerk(onlinePlayer));
                }
            }
            plugin.sync(() -> {
                if (playerInfo.getHasIsland()) {
                    WorldGuardHandler.protectIsland(onlinePlayer, playerInfo);
                    plugin.getIslandLogic().clearFlatland(onlinePlayer, playerInfo.getIslandLocation(), 400);
                }
                if (plugin.getWorldManager().isSkyAssociatedWorld(onlinePlayer.getWorld()) && !plugin.playerIsOnIsland(onlinePlayer)) {
                    // Check if banned
                    String islandName = WorldGuardHandler.getIslandNameAt(onlinePlayer.getLocation());
                    IslandInfo islandInfo = plugin.getIslandInfo(islandName);
                    if (islandInfo != null && islandInfo.isBanned(onlinePlayer)) {
                        onlinePlayer.sendMessage(tr("\u00a7eYou have been §cBANNED§e from {0}§e''s island.", islandInfo.getLeader()),
                            tr("\u00a7eSending you to spawn."));
                        plugin.getTeleportLogic().spawnTeleport(onlinePlayer, true);
                    } else if (islandInfo != null && islandInfo.isLocked()) {
                        if (!onlinePlayer.hasPermission("usb.mod.bypassprotection")) {
                            onlinePlayer.sendMessage(tr("\u00a7eThe island has been §cLOCKED§e.", islandInfo.getLeader()),
                                tr("\u00a7eSending you to spawn."));
                            plugin.getTeleportLogic().spawnTeleport(onlinePlayer, true);
                        }
                    }
                }
            }
            );
        }
    }

    public PlayerInfo getPlayerInfo(Player player) {
        return getPlayerInfo(player.getName());
    }

    public PlayerInfo getPlayerInfo(String playerName) {
        UUID uuid = playerDB.getUUIDFromName(playerName);
        return new PlayerInfo(databasePlayerCache.getUnchecked(uuid), plugin);
    }

    public PlayerInfo getPlayerInfo(UUID uuid) {
        if (plugin.isMaintenanceMode()) {
            return null;
        }

        try {
            return new PlayerInfo(databasePlayerCache.get(uuid), plugin);
        } catch (ExecutionException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void shutdown() {
        saveTask.cancel();
        flushCache();
        notificationManager.shutdown();
    }

    public long flushCache() {
        long size = databasePlayerCache.size();
        databasePlayerCache.invalidateAll();
        return size;
    }

    public int getSize() {
        String[] list = plugin.directoryPlayers.list();
        return list != null ? list.length : 0;
    }

    public @NotNull NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public us.talabrek.ultimateskyblock.api.model.Player getPlayer(UUID uuid) {
        try {
            return databasePlayerCache.get(uuid);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Unable to load player", ex);
        }
    }
}
