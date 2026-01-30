package us.talabrek.ultimateskyblock.handler.asyncworldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;

/**
 * Interface for various AWE version-adaptors.
 */
public interface AWEAdaptor {

    void onEnable(uSkyBlock plugin);

    void onDisable(uSkyBlock plugin);

    void loadIslandSchematic(File file, Location origin);

    EditSession createEditSession(World world, int maxBlocks);

    void regenerate(Region region, Runnable onCompletion);
}
