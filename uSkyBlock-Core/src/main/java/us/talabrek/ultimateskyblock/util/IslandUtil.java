package us.talabrek.ultimateskyblock.util;

import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Various Island centered utilities
 */
public enum IslandUtil {;

    public static FilenameFilter createIslandFilenameFilter(int spawnSize) {
        return new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name != null
                        && name.matches("-?[0-9]+,-?[0-9]+.yml")
                        && !"null.yml".equalsIgnoreCase(name)
                        && (spawnSize == 0  || !"0,0.yml".equalsIgnoreCase(name));
            }
        };
    }

    public static Location getIslandLocation(String islandName, World world, int islandHeight) {
        if (islandName == null || islandName.isEmpty()) {
            return null;
        }
        String[] cords = islandName.split(",");
        return new Location(world, Long.parseLong(cords[0], 10), islandHeight, Long.parseLong(cords[1], 10));
    }
}
