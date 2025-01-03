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
import java.util.logging.Level;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Holds the active players
 */
public class PlayerLogic {
    private static final Logger log = Logger.getLogger(PlayerLogic.class.getName());
    private static final PlayerInfo UNKNOWN_PLAYER = new PlayerInfo(PlayerDB.UNKNOWN_PLAYER_NAME, PlayerDB.UNKNOWN_PLAYER_UUID, uSkyBlock.getInstance());
    private final LoadingCache<UUID, PlayerInfo> playerCache;
    private final uSkyBlock plugin;
    private final BukkitTask saveTask;
    private final PlayerDB playerDB;
    private final NotificationManager notificationManager;

    public PlayerLogic(uSkyBlock plugin) {
        this.plugin = plugin;
        playerDB = plugin.getPlayerDB();
        playerCache = CacheBuilder
                .from(plugin.getConfig().getString("options.advanced.playerCache", "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m"))
                .removalListener((RemovalListener<UUID, PlayerInfo>) removal -> {
                    log.fine("Removing player-info for " + removal.getKey() + " from cache");
                    PlayerInfo playerInfo = removal.getValue();
                    if (playerInfo.isDirty()) {
                        playerInfo.saveToFile();
                    }
                })
                .build(new CacheLoader<>() {
                           @Override
                           public @NotNull PlayerInfo load(@NotNull UUID s) {
                               log.fine("Loading player-info from " + s + " into cache!");
                               return loadPlayerData(s);
                           }
                       }
                );
        long every = TimeUtil.secondsAsMillis(plugin.getConfig().getInt("options.advanced.player.saveEvery", 2*60));
        saveTask = plugin.async(this::saveDirtyToFiles, every, every);
        notificationManager = new NotificationManager(plugin);
    }

    private void saveDirtyToFiles() {
        // asMap.values() should NOT touch the cache.
        for (PlayerInfo pi : playerCache.asMap().values()) {
            if (pi.isDirty()) {
                pi.saveToFile();
            }
        }
    }

    private PlayerInfo loadPlayerData(UUID uuid) {
        if (UNKNOWN_PLAYER.getUniqueId().equals(uuid)) {
            return UNKNOWN_PLAYER;
        }
        return loadPlayerData(uuid, playerDB.getName(uuid));
    }

    private PlayerInfo loadPlayerData(UUID playerUUID, String playerName) {
        if (playerUUID == null) {
            playerUUID = PlayerDB.UNKNOWN_PLAYER_UUID;
        }
        if (playerName == null) {
            playerName = "__UNKNOWN__";
        }
        log.log(Level.FINER, "Loading player data for " + playerUUID + "/" + playerName);

        final PlayerInfo playerInfo = new PlayerInfo(playerName, playerUUID, plugin);

        final Player onlinePlayer = uSkyBlock.getInstance().getPlayerDB().getPlayer(playerUUID);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            if (playerInfo.getHasIsland()) {
                IslandInfo islandInfo = plugin.getIslandInfo(playerInfo);
                if (islandInfo != null) {
                    islandInfo.updatePermissionPerks(onlinePlayer, plugin.getPerkLogic().getPerk(onlinePlayer));
                }
            }
            plugin.sync(new Runnable() {
                        @Override
                        public void run() {
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
                    }
            );
        }
        return playerInfo;
    }

    public PlayerInfo getPlayerInfo(Player player) {
        return getPlayerInfo(player.getName());
    }

    public PlayerInfo getPlayerInfo(String playerName) {
        UUID uuid = playerDB.getUUIDFromName(playerName);
        return getPlayerInfo(uuid);
    }

    public PlayerInfo getPlayerInfo(UUID uuid) {
        if (plugin.isMaintenanceMode()) {
            return null;
        }
        try {
            return playerCache.get(uuid);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e); // Escalate - we need it in the server log
        }
    }

    public void loadPlayerDataAsync(final Player player) {
        plugin.async(new Runnable() {
            @Override
            public void run() {
                playerCache.refresh(player.getUniqueId());
            }
        });
    }

    public void removeActivePlayer(PlayerInfo pi) {
        playerCache.invalidate(pi.getPlayerId());
    }

    public void shutdown() {
        saveTask.cancel();
        flushCache();
        notificationManager.shutdown();
    }

    public long flushCache() {
        long size = playerCache.size();
        playerCache.invalidateAll();
        return size;
    }

    public int getSize() {
        String[] list = plugin.directoryPlayers.list();
        return list != null ? list.length : 0;
    }

    public @NotNull NotificationManager getNotificationManager() {
        return notificationManager;
    }
}
