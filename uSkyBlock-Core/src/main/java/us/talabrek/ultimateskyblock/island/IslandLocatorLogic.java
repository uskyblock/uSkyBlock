package us.talabrek.ultimateskyblock.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;

/**
 * Responsible for keeping track of and locating island locations for new islands.
 */
@Singleton
public class IslandLocatorLogic {
    private final Logger logger;
    private final WorldManager worldManager;
    private final Scheduler scheduler;
    private final OrphanLogic orphanLogic;
    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;
    private final File configFile;
    private final FileConfiguration config;
    private final Map<String, Instant> reservations = new ConcurrentHashMap<>();
    private Location lastIsland = null;
    private final Duration reservationTimeout;

    @Inject
    public IslandLocatorLogic(
        @NotNull uSkyBlock plugin,
        @NotNull @PluginDataDir Path pluginDir,
        @NotNull Logger logger,
        @NotNull WorldManager worldManager,
        @NotNull Scheduler scheduler,
        @NotNull OrphanLogic orphanLogic,
        @NotNull RuntimeConfigs runtimeConfigs
    ) {
        this.plugin = plugin;
        this.configFile = pluginDir.resolve("lastIslandConfig.yml").toFile();
        this.logger = logger;
        this.worldManager = worldManager;
        this.scheduler = scheduler;
        this.orphanLogic = orphanLogic;
        this.runtimeConfigs = runtimeConfigs;
        this.config = new YamlConfiguration();
        FileUtil.readConfig(config, configFile);
        // Backward compatibility
        if (!config.contains("options.general.lastIslandX") && plugin.getConfig().contains("options.general.lastIslandX")) {
            config.set("options.general.lastIslandX", plugin.getConfig().getInt("options.general.lastIslandX"));
            config.set("options.general.lastIslandZ", plugin.getConfig().getInt("options.general.lastIslandZ"));
            plugin.getConfig().set("options.general.lastIslandX", null);
            plugin.getConfig().set("options.general.lastIslandZ", null);
        }
        reservationTimeout = runtimeConfigs.current().island().reservationTimeout();
    }

    private Location getLastIsland() {
        int islandHeight = runtimeConfigs.current().island().height();
        int islandDistance = runtimeConfigs.current().island().distance();
        if (lastIsland == null) {
            lastIsland = new Location(worldManager.getWorld(),
                config.getInt("options.general.lastIslandX", 0), islandHeight,
                config.getInt("options.general.lastIslandZ", 0));
        }
        return LocationUtil.alignToDistance(lastIsland, islandDistance, islandHeight);
    }

    public synchronized Location getNextIslandLocation(Player player) {
        Location islandLocation = getNext(player);
        reserve(islandLocation);
        return islandLocation.clone();
    }

    private void reserve(Location islandLocation) {
        final String islandName = LocationUtil.getIslandName(islandLocation);
        final Instant timeStamp = Instant.now();
        reservations.put(islandName, timeStamp);
        scheduler.async(() -> {
            synchronized (reservations) {
                Instant tReserved = reservations.get(islandName);
                if (tReserved != null && tReserved == timeStamp) {
                    reservations.remove(islandName);
                }
            }
        }, reservationTimeout);
    }

    private synchronized Location getNext(Player player) {
        int islandHeight = runtimeConfigs.current().island().height();
        int islandDistance = runtimeConfigs.current().island().distance();
        Location last = getLastIsland();
        if (worldManager.isSkyWorld(player.getWorld()) && !plugin.islandInSpawn(player.getLocation())) {
            Location location = LocationUtil.alignToDistance(player.getLocation(), islandDistance, islandHeight);
            if (isAvailableLocation(location)) {
                sendTr(player, "Creating an island at your location", PRIMARY);
                return location;
            }
            Vector v = player.getLocation().getDirection().normalize();
            location = LocationUtil.alignToDistance(location.add(v.multiply(islandDistance)), islandDistance, islandHeight);
            if (isAvailableLocation(location)) {
                sendTr(player, "Creating an island <direction> of you",
                    component("direction", tr(LocationUtil.getCardinalDirection(player.getLocation().getYaw()), PRIMARY)));
                return location;
            }
        }
        Location next = orphanLogic.getNextValidOrphan(this);
        if (next == null) {
            next = last;
            // Ensure the found location is valid (or find one that is).
            while (!isAvailableLocation(next)) {
                next = nextIslandLocation(next, islandDistance, islandHeight);
            }
        }
        lastIsland = next;
        save();
        return next;
    }

    private void save() {
        final Location locationToSave = lastIsland;
        scheduler.async(() -> {
            try {
                config.set("options.general.lastIslandX", locationToSave.getBlockX());
                config.set("options.general.lastIslandZ", locationToSave.getBlockZ());
                config.save(configFile);
            } catch (IOException e) {
                logger.warning("Unable to save " + configFile);
            }
        });
    }

    public boolean isAvailableLocation(Location next) {
        return !(plugin.islandInSpawn(next) || plugin.islandAtLocation(next) || isReserved(next));
    }

    private boolean isReserved(Location next) {
        return reservations.containsKey(LocationUtil.getIslandName(next));
    }

    /**
     * <pre>
     *                            z
     *   x = -z                   ^                    x = z
     *        \        -x < z     |     x < z         /
     *           \                |                /
     *              \             |             /
     *                 \          |          /
     *                    \       |       /          x > z
     *        -x > z         \    |    /
     *                          \ | /
     *     -----------------------+-----------------------------> x
     *                          / | \
     *        -x > -z        /    |    \
     *        (x < z)     /       |       \          x > -z
     *                 /          |          \
     *              /             |             \
     *           /     -x < -z    |   x < -z       \
     *       x = z                |                x = -z
     *                            |
     *                            v
     * </pre>
     */
    static Location nextIslandLocation(final Location lastIsland, int d, int islandHeight) {
        LocationUtil.alignToDistance(lastIsland, d, islandHeight);
        int x = lastIsland.getBlockX();
        int z = lastIsland.getBlockZ();
        if (x < z) {
            if (-1 * x < z) {
                x += d;
            } else {
                z += d;
            }
        } else if (x > z) {
            if (-1 * x >= z) {
                x -= d;
            } else {
                z -= d;
            }
        } else { // x == z
            if (x <= 0) {
                z += d;
            } else {
                z -= d;
            }
        }
        lastIsland.setX(x);
        lastIsland.setZ(z);
        return lastIsland;
    }
}
