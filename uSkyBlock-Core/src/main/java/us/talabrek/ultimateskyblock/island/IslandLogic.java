package us.talabrek.ultimateskyblock.island;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dk.lockfuglsang.minecraft.file.FileUtil;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.IslandLevel;
import us.talabrek.ultimateskyblock.api.IslandRank;
import us.talabrek.ultimateskyblock.api.event.uSkyBlockEvent;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;
import us.talabrek.ultimateskyblock.handler.WorldEditHandler;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.handler.task.WorldEditClearFlatlandTask;
import us.talabrek.ultimateskyblock.island.level.IslandScore;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.TeleportLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.IslandUtil;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static org.bukkit.Material.BEDROCK;

/**
 * Responsible for island creation, locating locations, purging, clearing etc.
 */
@Singleton
public class IslandLogic {
    private final Logger logger;
    private final uSkyBlock plugin;
    private final WorldManager worldManager;
    private final TeleportLogic teleportLogic;
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final Path directoryIslands;
    private final OrphanLogic orphanLogic;
    private final PlayerDB playerDB;

    private final LoadingCache<String, IslandInfo> cache;
    private final boolean showMembers;
    private final boolean flatlandFix;
    private final boolean useDisplayNames;
    private final BukkitTask saveTask;
    private final double topTenCutoff;

    private volatile Instant lastUpdated = Instant.MIN;
    private final List<IslandLevel> ranks = new ArrayList<>();

    @Inject
    public IslandLogic(
        @NotNull Logger logger,
        @NotNull uSkyBlock plugin,
        @NotNull WorldManager worldManager,
        @NotNull TeleportLogic teleportLogic,
        @NotNull Scheduler scheduler,
        @NotNull PluginConfig config,
        @NotNull @PluginDataDir Path dataPath,
        @NotNull OrphanLogic orphanLogic,
        @NotNull PlayerDB playerDB
    ) {
        this.logger = logger;
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.teleportLogic = teleportLogic;
        this.scheduler = scheduler;
        this.config = config;
        this.playerDB = playerDB;
        Path islandDirectory = dataPath.resolve("islands");
        try {
            Files.createDirectories(islandDirectory);
        } catch (IOException e) {
            logger.severe("Unable to create island directory: " + islandDirectory);
        }
        this.directoryIslands = islandDirectory;
        this.orphanLogic = orphanLogic;
        this.showMembers = config.getYamlConfig().getBoolean("options.island.topTenShowMembers", true);
        this.flatlandFix = config.getYamlConfig().getBoolean("options.island.fixFlatland", false);
        this.useDisplayNames = config.getYamlConfig().getBoolean("options.advanced.useDisplayNames", false);
        topTenCutoff = config.getYamlConfig().getDouble("options.advanced.topTenCutoff", config.getYamlConfig().getDouble("options.advanced.purgeLevel", 10));
        cache = CacheBuilder
            .from(config.getYamlConfig().getString("options.advanced.islandCache",
                "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m"))
            .removalListener((RemovalListener<String, IslandInfo>) removal -> {
                logger.fine("Removing island-info " + removal.getKey() + " from cache");
                removal.getValue().saveToFile();
            })
            .build(new CacheLoader<>() {
                @Override
                public @NotNull IslandInfo load(@NotNull String islandName) {
                    logger.fine("Loading island-info " + islandName + " to cache!");
                    return new IslandInfo(islandName, plugin, directoryIslands);
                }
            });
        Duration every = Duration.ofSeconds(config.getYamlConfig().getInt("options.advanced.island.saveEvery", 30));
        saveTask = scheduler.async(this::saveDirtyToFiles, every, every);
    }

    private void saveDirtyToFiles() {
        // asMap.values() should NOT touch the cache.
        for (IslandInfo islandInfo : cache.asMap().values()) {
            if (islandInfo.isDirty()) {
                islandInfo.saveToFile();
            }
        }
    }

    public synchronized IslandInfo getIslandInfo(String islandName) {
        if (islandName == null || plugin.isMaintenanceMode()) {
            return null;
        }
        try {
            return cache.get(islandName);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unable to load island", e);
        }
    }

    public IslandInfo getIslandInfo(PlayerInfo playerInfo) {
        if (playerInfo != null && playerInfo.getHasIsland()) {
            return getIslandInfo(playerInfo.locationForParty());
        }
        return null;
    }

    public void clearIsland(final Location loc, final Runnable afterDeletion) {
        logger.log(Level.FINE, "clearing island at {0}", loc);
        Runnable clearNether = () -> {
            Location netherIsland = getNetherLocation(loc);
            ProtectedRegion netherRegion = WorldGuardHandler.getNetherRegionAt(netherIsland);
            if (netherRegion != null) {
                for (Player player : WorldGuardHandler.getPlayersInRegion(netherIsland.getWorld(), netherRegion)) {
                    if (player != null && player.isOnline() && worldManager.isSkyNether(player.getWorld()) && !player.isFlying()) {
                        player.sendMessage(tr("\u00a7cThe island owning this piece of nether is being deleted! Sending you to spawn."));
                        teleportLogic.spawnTeleport(player, true);
                    }
                }
                WorldEditHandler.clearIsland(netherIsland.getWorld(), netherRegion, afterDeletion);
            } else {
                afterDeletion.run();
            }
        };
        World skyBlockWorld = worldManager.getWorld();
        ProtectedRegion region = WorldGuardHandler.getIslandRegionAt(loc);
        if (region != null) {
            for (Player player : WorldGuardHandler.getPlayersInRegion(worldManager.getWorld(), region)) {
                if (player != null && player.isOnline() && worldManager.isSkyWorld(player.getWorld()) && !player.isFlying()) {
                    player.sendMessage(tr("\u00a7cThe island you are on is being deleted! Sending you to spawn."));
                    teleportLogic.spawnTeleport(player, true);
                }
            }
            WorldEditHandler.clearIsland(skyBlockWorld, region, clearNether);
        } else {
            logger.log(Level.WARNING, "Trying to delete an island - with no WG region! ({0})", LocationUtil.asString(loc));
            clearNether.run();
        }
    }

    private Location getNetherLocation(Location loc) {
        Location netherIsland = loc.clone();
        netherIsland.setWorld(worldManager.getNetherWorld());
        netherIsland.setY(loc.getY() / 2);
        return netherIsland;
    }

    public boolean clearFlatland(final CommandSender sender, final Location loc, Duration delay) {
        if (loc == null) {
            return false;
        }
        if (delay.isPositive() && !flatlandFix) {
            return false; // Skip
        }
        Runnable runnable = () -> {
            final World w = loc.getWorld();
            final int px = loc.getBlockX();
            final int pz = loc.getBlockZ();
            final int py = 0;
            final int range = Math.max(Settings.island_protectionRange, Settings.island_distance) + 1;
            final int radius = range / 2;
            // 5 sampling points...
            if (w.getBlockAt(px, py, pz).getType() == BEDROCK
                || w.getBlockAt(px + radius, py, pz + radius).getType() == BEDROCK
                || w.getBlockAt(px + radius, py, pz - radius).getType() == BEDROCK
                || w.getBlockAt(px - radius, py, pz + radius).getType() == BEDROCK
                || w.getBlockAt(px - radius, py, pz - radius).getType() == BEDROCK) {
                sender.sendMessage(String.format("\u00a7c-----------------------------------\n\u00a7cFlatland detected under your island!\n\u00a7e Clearing it in %s, stay clear.\n\u00a7c-----------------------------------\n", TimeUtil.durationAsString(delay)));
                scheduler.sync(new WorldEditClearFlatlandTask(scheduler, config, worldManager, logger, sender,
                    new CuboidRegion(BlockVector3.at(px - radius, 0, pz - radius),
                        BlockVector3.at(px + radius, 4, pz + radius)),
                    "\u00a7eFlatland was cleared under your island (%s). Take care."), delay);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            scheduler.sync(runnable);
        }
        return false;
    }

    public void displayTopTen(final CommandSender sender, int page) {
        synchronized (ranks) {
            int maxpage = ((ranks.size() - 1) / 10) + 1;
            if (page > maxpage) {
                page = maxpage;
            }
            if (page < 1) {
                page = 1;
            }
            sender.sendMessage(tr("\u00a7eWALL OF FAME (page {0} of {1}):", page, maxpage));
            if (ranks.isEmpty()) {
                if (Settings.island_useTopTen) {
                    sender.sendMessage(tr("\u00a74Top ten list is empty! Only islands above level {0} is considered.", topTenCutoff));
                } else {
                    sender.sendMessage(tr("\u00a74Island level has been disabled, contact an administrator."));
                }
            }
            int place = 1;
            PlayerInfo playerInfo = plugin.getPlayerInfo(sender.getName());
            IslandRank rank = null;
            if (playerInfo != null && playerInfo.getHasIsland()) {
                rank = getRank(playerInfo.locationForParty());
            }
            int offset = (page - 1) * 10;
            place += offset;
            for (final IslandLevel level : ranks.subList(offset, Math.min(ranks.size(), 10 * page))) {
                String members = "";
                if (showMembers && !level.getMembers().isEmpty()) {
                    members = Arrays.toString(level.getMembers().toArray(new String[0]));
                }
                String message = String.format(tr("\u00a7a#%2d \u00a77(%5.2f): \u00a7e%s \u00a77%s"),
                    place, level.getScore(), level.getLeaderName(), members);
                if (sender instanceof Player target) {
                    String warpString = getJsonWarpString(
                        message,
                        tr("Click to warp to the island!"),
                        String.format("/is w %s", level.getLeaderName())
                    );
                    uSkyBlock.getInstance().execCommand(target, "console:tellraw " +
                        target.getName() + " " + warpString, false);
                } else {
                    sender.sendMessage(message);
                }


                place++;
            }
            if (rank != null) {
                sender.sendMessage(tr("\u00a7eYour rank is: \u00a7f{0}", rank.getRank()));
            }
        }

    }

    private String getJsonWarpString(String text, String hoverText, String command) {
        Map<String, Object> hoverEvent = new HashMap<>();
        hoverEvent.put("action", "show_text");
        hoverEvent.put("value", hoverText);

        Map<String, Object> clickEvent = new HashMap<>();
        clickEvent.put("action", "run_command");
        clickEvent.put("value", command);

        Map<String, Object> rootMap = new HashMap<>();
        rootMap.put("text", text);
        rootMap.put("hoverEvent", hoverEvent);
        rootMap.put("clickEvent", clickEvent);

        return new GsonBuilder().create().toJson(rootMap);
    }

    public void showTopTen(final CommandSender sender, final int page) {
        Instant now = Instant.now();
        if (now.isAfter(lastUpdated.plus(Settings.island_topTenTimeout)) || (sender.hasPermission("usb.admin.topten") || sender.isOp())) {
            lastUpdated = now;
            scheduler.async(() -> {
                generateTopTen(sender);
                displayTopTen(sender, page);
            });
        } else {
            displayTopTen(sender, page);
        }
    }

    public List<IslandLevel> getRanks(int offset, int length) {
        synchronized (ranks) {
            int size = ranks.size();
            if (size <= offset) {
                return Collections.emptyList();
            }
            return ranks.subList(offset, Math.min(size - offset, length));
        }
    }

    public void generateTopTen(final CommandSender sender) {
        List<IslandLevel> topTen = new ArrayList<>();
        final String[] listOfFiles = directoryIslands.toFile().list(IslandUtil.createIslandFilenameFilter());
        for (String file : listOfFiles) {
            String islandName = FileUtil.getBasename(file);
            try {
                boolean wasLoaded = cache.getIfPresent(islandName) != null;
                IslandInfo islandInfo = getIslandInfo(islandName);
                double level = islandInfo != null ? islandInfo.getLevel() : 0;
                if (islandInfo != null && level > topTenCutoff && !islandInfo.ignore()) {
                    IslandLevel islandLevel = createIslandLevel(islandInfo, level);
                    topTen.add(islandLevel);
                }
                if (!wasLoaded) {
                    cache.invalidate(islandName);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error during rank generation", e);
            }
        }
        Collections.sort(topTen);
        synchronized (ranks) {
            lastUpdated = Instant.now();
            ranks.clear();
            ranks.addAll(topTen);
        }
        plugin.fireChangeEvent(sender, uSkyBlockEvent.Cause.RANK_UPDATED);
    }

    private IslandLevel createIslandLevel(IslandInfo islandInfo, double level) {
        String partyLeader = islandInfo.getLeader();
        String partyLeaderName = partyLeader;
        List<String> memberList = new ArrayList<>(islandInfo.getMembers());
        memberList.remove(partyLeader);
        List<String> names = new ArrayList<>();
        if (useDisplayNames) {
            partyLeaderName = playerDB.getDisplayName(partyLeader);
            for (String name : memberList) {
                String displayName = playerDB.getDisplayName(name);
                if (displayName != null) {
                    names.add(displayName);
                }
            }
        } else {
            names = memberList;
        }
        return new IslandLevel(islandInfo.getName(), partyLeaderName, names, level);
    }

    public synchronized IslandInfo createIslandInfo(String location, String player) {
        IslandInfo info = getIslandInfo(location);
        info.resetIslandConfig(player);
        return info;
    }

    public synchronized void deleteIslandConfig(final String location) {
        try {
            IslandInfo islandInfo = cache.get(location);
            updateRank(islandInfo, new IslandScore(0, Collections.emptyList()));
            if (islandInfo.exists()) {
                islandInfo.delete();
            }
            cache.invalidate(location);
            orphanLogic.addOrphan(location);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unable to delete island " + location, e);
        }
    }

    public synchronized void removeIslandFromMemory(String islandName) {
        cache.invalidate(islandName);
    }

    public void updateRank(IslandInfo islandInfo, IslandScore score) {
        synchronized (ranks) {
            IslandLevel islandLevel = createIslandLevel(islandInfo, score.getScore());
            ranks.remove(islandLevel);
            ranks.add(islandLevel);
            Collections.sort(ranks);
        }
    }

    public boolean hasIsland(Location loc) {
        return loc == null || new File(directoryIslands.toFile(), LocationUtil.getIslandName(loc) + ".yml").exists();
    }

    public IslandRank getRank(String islandName) {
        ArrayList<IslandLevel> rankList = new ArrayList<>(ranks);
        for (int i = 0; i < rankList.size(); i++) {
            IslandLevel level = rankList.get(i);
            if (level.getIslandName().equalsIgnoreCase(islandName)) {
                return new IslandRank(level, i + 1);
            }
        }
        return null;
    }

    public boolean purge(String islandName) {
        IslandInfo islandInfo = getIslandInfo(islandName);
        if (islandInfo != null && !islandInfo.ignore()) {
            for (UUID member : islandInfo.getMemberUUIDs()) {
                PlayerInfo pi = plugin.getPlayerInfo(member);
                if (pi != null) {
                    islandInfo.removeMember(pi);
                }
            }
            WorldGuardHandler.removeIslandRegion(islandName);
            deleteIslandConfig(islandName);
            return true;
        }
        return false;
    }

    public void shutdown() {
        saveTask.cancel();
        flushCache();
        saveDirtyToFiles();
    }

    public long flushCache() {
        long size = cache.size();
        cache.invalidateAll(); // Flush to files
        return size;
    }

    public int getSize() {
        try (var stream = Files.list(directoryIslands)) {
            return (int) stream.count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path getIslandDirectory() {
        return directoryIslands;
    }
}
