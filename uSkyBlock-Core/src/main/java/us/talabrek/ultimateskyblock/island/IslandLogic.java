package us.talabrek.ultimateskyblock.island;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.gson.GsonBuilder;
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
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.IslandLevel;
import us.talabrek.ultimateskyblock.api.IslandRank;
import us.talabrek.ultimateskyblock.api.event.uSkyBlockEvent;
import us.talabrek.ultimateskyblock.api.model.Island;
import us.talabrek.ultimateskyblock.handler.WorldEditHandler;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.handler.task.WorldEditClearFlatlandTask;
import us.talabrek.ultimateskyblock.island.level.IslandScore;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.IslandUtil;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.util.PlayerUtil;

import java.io.File;
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
public class IslandLogic {
    private static final Logger log = Logger.getLogger(IslandLogic.class.getName());
    private final uSkyBlock plugin;
    private final File directoryIslands;
    private final OrphanLogic orphanLogic;

    private final LoadingCache<String, IslandInfo> cache;
    private final LoadingCache<UUID, Island> islandCache;
    private final boolean showMembers;
    private final boolean flatlandFix;
    private final boolean useDisplayNames;
    private final BukkitTask saveTask;
    private final double topTenCutoff;

    private volatile long lastGenerate = 0;
    private final List<IslandLevel> ranks = new ArrayList<>();

    public IslandLogic(uSkyBlock plugin, File directoryIslands, OrphanLogic orphanLogic) {
        this.plugin = plugin;
        this.directoryIslands = directoryIslands;
        this.orphanLogic = orphanLogic;
        this.showMembers = plugin.getConfig().getBoolean("options.island.topTenShowMembers", true);
        this.flatlandFix = plugin.getConfig().getBoolean("options.island.fixFlatland", false);
        this.useDisplayNames = plugin.getConfig().getBoolean("options.advanced.useDisplayNames", false);
        topTenCutoff = plugin.getConfig().getDouble("options.advanced.topTenCutoff", plugin.getConfig().getDouble("options.advanced.purgeLevel", 10));
        cache = CacheBuilder
                .from(plugin.getConfig().getString("options.advanced.islandCache",
                        "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m"))
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull IslandInfo load(@NotNull String islandName) {
                        log.fine("Loading island-info " + islandName + " to cache!");
                        return new IslandInfo(islandName, plugin);
                    }
                });

        islandCache = CacheBuilder
            .from(plugin.getConfig().getString("options.advanced.islandCache",
                "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m"))
            .removalListener((RemovalListener<UUID, Island>) removal -> {
                plugin.getLog4JLogger().info("Unloading island {} from database cache.", removal.getKey());
                plugin.getStorage().saveIsland(removal.getValue());
            })
            .build(new CacheLoader<>() {
                @Override
                public @NotNull Island load(@NotNull UUID uuid) {
                    plugin.getLog4JLogger().info("Loading island {} from database cache.", uuid);
                    return plugin.getStorage().getIsland(uuid).join();
                }
            });

        long every = TimeUtil.secondsAsMillis(plugin.getConfig().getInt("options.advanced.island.saveEvery", 30));
        saveTask = plugin.async(this::saveDirtyToFiles, every, every);
    }

    private void saveDirtyToFiles() {
        islandCache.asMap().values().forEach(island -> {
            if (island.isDirty()) plugin.getStorage().saveIsland(island);
        });
    }

    public IslandInfo getIslandInfo(UUID islandUuid) {
        try {
            Island island = islandCache.get(islandUuid);
            return cache.get(island.getName());
        } catch (ExecutionException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load island " + islandUuid + " from cache", ex);
        }

        return null;
    }

    public synchronized IslandInfo getIslandInfo(String islandName) {
        if (islandName == null || plugin.isMaintenanceMode()) {
            return null;
        }
        UUID islandUuid = plugin.getStorage().getIslandByName(islandName).join();
        if (islandUuid != null) {
            try {
                Island island = islandCache.get(islandUuid);
                return new IslandInfo(island, plugin);
            } catch (ExecutionException ex) {
                plugin.getLog4JLogger().error("Failed to load island {} from cache.", islandName, ex);
            }
        }

        return null;
    }

    public IslandInfo getIslandInfo(PlayerInfo playerInfo) {
        if (playerInfo != null && playerInfo.getHasIsland()) {
            return getIslandInfo(playerInfo.locationForParty());
        }
        return null;
    }

    public Island getIsland(UUID uuid) {
        try {
            return islandCache.get(uuid);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unable to load island", e);
        } catch (CacheLoader.InvalidCacheLoadException cacheMiss) {
            // This is expected if the island doesn't exist / the database returns NULL.
            // TODO: Return NULL for now, should be replaced by some nicer handling like an Optional in the future.
            return null;
        }
    }

    public void clearIsland(final Location loc, final Runnable afterDeletion) {
        log.log(Level.FINE, "clearing island at {0}", loc);
        Runnable clearNether = new Runnable() {
            @Override
            public void run() {
                Location netherIsland = getNetherLocation(loc);
                ProtectedRegion netherRegion = WorldGuardHandler.getNetherRegionAt(netherIsland);
                if (netherRegion != null) {
                    for (Player player : WorldGuardHandler.getPlayersInRegion(netherIsland.getWorld(), netherRegion)) {
                        if (player != null && player.isOnline() && plugin.getWorldManager().isSkyNether(player.getWorld()) && !player.isFlying()) {
                            player.sendMessage(tr("\u00a7cThe island owning this piece of nether is being deleted! Sending you to spawn."));
                            plugin.getTeleportLogic().spawnTeleport(player, true);
                        }
                    }
                    WorldEditHandler.clearIsland(netherIsland.getWorld(), netherRegion, afterDeletion);
                } else {
                    afterDeletion.run();
                }
            }
        };
        World skyBlockWorld = plugin.getWorldManager().getWorld();
        ProtectedRegion region = WorldGuardHandler.getIslandRegionAt(loc);
        if (region != null) {
            for (Player player : WorldGuardHandler.getPlayersInRegion(plugin.getWorldManager().getWorld(), region)) {
                if (player != null && player.isOnline() && plugin.getWorldManager().isSkyWorld(player.getWorld()) && !player.isFlying()) {
                    player.sendMessage(tr("\u00a7cThe island you are on is being deleted! Sending you to spawn."));
                    plugin.getTeleportLogic().spawnTeleport(player, true);
                }
            }
            WorldEditHandler.clearIsland(skyBlockWorld, region, clearNether);
        } else {
            log.log(Level.WARNING, "Trying to delete an island - with no WG region! ({0})", LocationUtil.asString(loc));
            clearNether.run();
        }
    }

    private Location getNetherLocation(Location loc) {
        Location netherIsland = loc.clone();
        netherIsland.setWorld(plugin.getWorldManager().getNetherWorld());
        netherIsland.setY(loc.getY()/2);
        return netherIsland;
    }

    public boolean clearFlatland(final CommandSender sender, final Location loc, final int delay) {
        if (loc == null) {
            return false;
        }
        if (delay > 0 && !flatlandFix) {
            return false; // Skip
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final World w = loc.getWorld();
                final int px = loc.getBlockX();
                final int pz = loc.getBlockZ();
                final int py = 0;
                final int range = Math.max(Settings.island_protectionRange, Settings.island_distance) + 1;
                final int radius = range/2;
                // 5 sampling points...
                if (w.getBlockAt(px, py, pz).getType() == BEDROCK
                        || w.getBlockAt(px+radius, py, pz+radius).getType() == BEDROCK
                        || w.getBlockAt(px+radius, py, pz-radius).getType() == BEDROCK
                        || w.getBlockAt(px-radius, py, pz+radius).getType() == BEDROCK
                        || w.getBlockAt(px-radius, py, pz-radius).getType() == BEDROCK)
                {
                    sender.sendMessage(String.format("\u00a7c-----------------------------------\n\u00a7cFlatland detected under your island!\n\u00a7e Clearing it in %s, stay clear.\n\u00a7c-----------------------------------\n", TimeUtil.ticksAsString(delay)));
                    new WorldEditClearFlatlandTask(plugin, sender, new CuboidRegion(BlockVector3.at(px-radius, 0, pz-radius),
                            BlockVector3.at(px+radius, 4, pz+radius)),
                            "\u00a7eFlatland was cleared under your island (%s). Take care.").runTaskLater(plugin, delay);
                }
            }
        };
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            plugin.sync(runnable);
        }
        return false;
    }

    public void displayTopTen(final CommandSender sender, int page) {
        synchronized (ranks) {
            int maxpage = (( ranks.size()-1) / 10) + 1;
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
            int offset = (page-1) * 10;
            place += offset;
            for (final IslandLevel level : ranks.subList(offset, Math.min(ranks.size(), 10*page))) {
                String members = "";
                if (showMembers && !level.getMembers().isEmpty()) {
                    members = Arrays.toString(level.getMembers().toArray(new String[level.getMembers().size()]));
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
        long t = System.currentTimeMillis();
        if (t > (lastGenerate + (Settings.island_topTenTimeout*60000)) || (sender.hasPermission("usb.admin.topten") || sender.isOp())) {
            lastGenerate = t;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
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
            return ranks.subList(offset, Math.min(size-offset, length));
        }
    }

    public void generateTopTen(final CommandSender sender) {
        List<IslandLevel> topTen = new ArrayList<>();
        final String[] listOfFiles = directoryIslands.list(IslandUtil.createIslandFilenameFilter());
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
                plugin.getLogger().log(Level.WARNING, "Error during rank generation", e);
            }
        }
        Collections.sort(topTen);
        synchronized (ranks) {
            lastGenerate = System.currentTimeMillis();
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
        	partyLeaderName = PlayerUtil.getPlayerDisplayName(partyLeader);
        	for (String name : memberList) {
	            String displayName = PlayerUtil.getPlayerDisplayName(name);
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
        UUID leader = plugin.getPlayerDB().getUUIDFromName(player);
        UUID islandAtLocation = plugin.getStorage().getIslandByName(location).join();

        IslandInfo info;
        if (islandAtLocation == null) {
            Island island = new Island(location, leader);
            islandCache.put(island.getUuid(), island);
            info = new IslandInfo(island, plugin);
            info.save();

            plugin.getLog4JLogger().info("Created new island {} ({})", island.getUuid(), island.getName());
        } else {
            info = getIslandInfo(islandAtLocation);
        }

        info.resetIslandConfig(leader);
        return info;
    }

    public synchronized void deleteIslandConfig(final String location) {
        try {
            UUID islandIdentifier = plugin.getStorage().getIslandByName(location).join();

            IslandInfo islandInfo = cache.get(location);
            Island island = islandCache.get(islandIdentifier);
            updateRank(islandInfo, new IslandScore(0, Collections.emptyList()));

            cache.invalidate(location);
            islandCache.invalidate(islandIdentifier);
            plugin.getStorage().deleteIsland(island);

            orphanLogic.addOrphan(location);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unable to delete island " + location, e);
        }
    }

    public synchronized void removeIslandFromMemory(String islandName) {
        cache.invalidate(islandName);
        UUID islandIdentifier = plugin.getStorage().getIslandByName(islandName).join();
        islandCache.invalidate(islandIdentifier);
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
        UUID islandIdentifier = plugin.getStorage().getIslandByName(LocationUtil.getIslandName(loc)).join();
        return loc == null || islandIdentifier != null;
    }

    public IslandRank getRank(String islandName) {
        ArrayList<IslandLevel> rankList = new ArrayList<>(ranks);
        for (int i = 0; i < rankList.size(); i++) {
            IslandLevel level = rankList.get(i);
            if (level.getIslandName().equalsIgnoreCase(islandName)) {
                return new IslandRank(level, i+1);
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
        islandCache.invalidateAll();
        return size;
    }

    public int getSize() {
        return plugin.getStorage().getIslandCount().join();
    }
}
