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
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Holds the active players
 */
@Singleton
public class PlayerLogic {
    private final LoadingCache<UUID, us.talabrek.ultimateskyblock.api.model.Player> databasePlayerCache;
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

                    scheduler.sync(() -> loadPlayerData(uuid), Duration.ofSeconds(1));
                    return plugin.getStorage().getPlayer(uuid).join();
                }
            });

        Duration every = Duration.ofSeconds(plugin.getConfig().getInt("options.advanced.player.saveEvery", 2 * 60));
        this.saveTask = scheduler.async(this::saveDirtyToFiles, every, every);
    }

    private void saveDirtyToFiles() {
        databasePlayerCache.asMap().values().forEach(player -> {
            if (player.isDirty()) plugin.getStorage().savePlayer(player);
        });
    }

    private void loadPlayerData(UUID uuid) {
        final Player onlinePlayer = plugin.getPlayerDB().getPlayer(uuid);

        if (databasePlayerCache.getIfPresent(uuid) == null) return;
        final PlayerInfo playerInfo = new PlayerInfo(getPlayer(uuid), plugin, playerDataDirectory);

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
                    plugin.getIslandLogic().clearFlatland(onlinePlayer, playerInfo.getIslandLocation(), Duration.ofSeconds(20));
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
        return getPlayerInfo(player.getUniqueId());
    }

    public PlayerInfo getPlayerInfo(String playerName) {
        UUID uuid = playerDB.getUUIDFromName(playerName);
        return getPlayerInfo(uuid);
    }

    public PlayerInfo getPlayerInfo(UUID uuid) {
        if (plugin.isMaintenanceMode()) {
            return null;
        }

        var player = getPlayer(uuid);
        return (player != null) ? new PlayerInfo(player, plugin, playerDataDirectory) : null;
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
        try (var stream = Files.list(playerDataDirectory)) {
            return (int) stream.count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public us.talabrek.ultimateskyblock.api.model.Player getPlayer(UUID uuid) {
        try {
            return databasePlayerCache.get(uuid);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Unable to load player", ex);
        } catch (CacheLoader.InvalidCacheLoadException cacheMiss) {
            // This is expected if the player doesn't exist / the database returns NULL.
            // TODO: Return NULL for now, should be replaced by some nicer handling like an Optional in the future.
            return null;
        }
    }
}
