package us.talabrek.ultimateskyblock.player;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Holds the active players
 */
@Singleton
public class PlayerLogic {
    private final LoadingCache<UUID, PlayerInfo> playerCache;
    private final uSkyBlock plugin;
    private final BukkitTask saveTask;
    private final PlayerDB playerDB;
    private final PerkLogic perkLogic;
    private final IslandLogic islandLogic;
    private final WorldManager worldManager;
    private final TeleportLogic teleportLogic;
    private final Scheduler scheduler;
    private final NotificationManager notificationManager;
    private final Logger logger;
    private final Path playerDataDirectory;

    @Inject
    public PlayerLogic(
        @NotNull uSkyBlock plugin,
        @NotNull PluginConfig config,
        @NotNull PlayerDB playerDB,
        @NotNull Logger logger,
        @NotNull PerkLogic perkLogic,
        @NotNull IslandLogic islandLogic,
        @NotNull WorldManager worldManager,
        @NotNull TeleportLogic teleportLogic,
        @NotNull Scheduler scheduler,
        @NotNull NotificationManager notificationManager,
        @NotNull @PluginDataDir Path pluginDataDir
    ) {
        this.plugin = plugin;
        this.playerDB = playerDB;
        this.perkLogic = perkLogic;
        this.islandLogic = islandLogic;
        this.worldManager = worldManager;
        this.teleportLogic = teleportLogic;
        this.scheduler = scheduler;
        this.notificationManager = notificationManager;
        this.logger = logger;
        this.playerDataDirectory = pluginDataDir.resolve("players");
        try {
            Files.createDirectories(playerDataDirectory);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create player data directory", e);
        }

        this.playerCache = CacheBuilder
            .from(config.getYamlConfig().getString("options.advanced.playerCache", "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m"))
            .removalListener((RemovalListener<UUID, PlayerInfo>) removal -> {
                logger.fine("Removing player-info for " + removal.getKey() + " from cache");
                PlayerInfo playerInfo = removal.getValue();
                if (playerInfo.isDirty()) {
                    playerInfo.saveToFile();
                }
            })
            .build(new CacheLoader<>() {
                       @Override
                       public @NotNull PlayerInfo load(@NotNull UUID s) {
                           logger.fine("Loading player-info from " + s + " into cache!");
                           return loadPlayerData(s);
                       }
                   }
            );
        Duration every = Duration.ofSeconds(plugin.getConfig().getInt("options.advanced.player.saveEvery", 2 * 60));
        this.saveTask = scheduler.async(this::saveDirtyToFiles, every, every);
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
        if (PlayerDB.UNKNOWN_PLAYER_UUID.equals(uuid)) {
            return loadUnknownPlayer();
        }
        return loadPlayerData(uuid, playerDB.getName(uuid));
    }

    private PlayerInfo loadUnknownPlayer() {
        return new PlayerInfo(PlayerDB.UNKNOWN_PLAYER_NAME, PlayerDB.UNKNOWN_PLAYER_UUID, plugin, playerDataDirectory);
    }

    private PlayerInfo loadPlayerData(UUID playerUUID, String playerName) {
        if (playerUUID == null) {
            playerUUID = PlayerDB.UNKNOWN_PLAYER_UUID;
        }
        if (playerName == null) {
            playerName = "__UNKNOWN__";
        }
        logger.log(Level.FINER, "Loading player data for " + playerUUID + "/" + playerName);

        final PlayerInfo playerInfo = new PlayerInfo(playerName, playerUUID, plugin, playerDataDirectory);

        final Player onlinePlayer = uSkyBlock.getInstance().getPlayerDB().getPlayer(playerUUID);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            if (playerInfo.getHasIsland()) {
                IslandInfo islandInfo = plugin.getIslandInfo(playerInfo);
                if (islandInfo != null) {
                    islandInfo.updatePermissionPerks(onlinePlayer, perkLogic.getPerk(onlinePlayer));
                }
            }
            scheduler.sync(() -> {
                    if (playerInfo.getHasIsland()) {
                        WorldGuardHandler.protectIsland(onlinePlayer, playerInfo);
                        islandLogic.clearFlatland(onlinePlayer, playerInfo.getIslandLocation(), Duration.ofSeconds(20));
                    }
                    if (worldManager.isSkyAssociatedWorld(onlinePlayer.getWorld()) && !plugin.playerIsOnIsland(onlinePlayer)) {
                        // Check if banned
                        String islandName = WorldGuardHandler.getIslandNameAt(onlinePlayer.getLocation());
                        IslandInfo islandInfo = plugin.getIslandInfo(islandName);
                        if (islandInfo != null && islandInfo.isBanned(onlinePlayer)) {
                            onlinePlayer.sendMessage(tr("\u00a7eYou have been §cBANNED§e from {0}§e''s island.", islandInfo.getLeader()),
                                tr("\u00a7eSending you to spawn."));
                            teleportLogic.spawnTeleport(onlinePlayer, true);
                        } else if (islandInfo != null && islandInfo.isLocked()) {
                            if (!onlinePlayer.hasPermission("usb.mod.bypassprotection")) {
                                onlinePlayer.sendMessage(tr("\u00a7eThe island has been §cLOCKED§e.", islandInfo.getLeader()),
                                    tr("\u00a7eSending you to spawn."));
                                teleportLogic.spawnTeleport(onlinePlayer, true);
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
        scheduler.async(() -> playerCache.refresh(player.getUniqueId()));
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
        try (var stream = Files.list(playerDataDirectory)) {
            return (int) stream.count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull NotificationManager getNotificationManager() {
        return notificationManager;
    }
}
