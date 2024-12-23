package us.talabrek.ultimateskyblock.handler;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.World;
import dk.lockfuglsang.minecraft.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import us.talabrek.ultimateskyblock.handler.asyncworldedit.AWEAdaptor;
import us.talabrek.ultimateskyblock.handler.task.WEPasteSchematic;
import us.talabrek.ultimateskyblock.player.PlayerPerk;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.util.logging.Level;

import static us.talabrek.ultimateskyblock.util.LogUtil.log;

/**
 * Handles integration with AWE.
 * Very HACKY and VERY unstable.
 *
 * Only kept as a cosmetic measure, to at least try to give the players some feedback.
 */
public enum AsyncWorldEditHandler {;
    private static AWEAdaptor adaptor = null;

    public static void onEnable(uSkyBlock plugin) {
        getAWEAdaptor().onEnable(plugin);
    }

    public static void onDisable(uSkyBlock plugin) {
        getAWEAdaptor().onDisable(plugin);
        adaptor = null;
    }

    public static EditSession createEditSession(World world, int maxblocks) {
        return getAWEAdaptor().createEditSession(world, maxblocks);
    }

    public static void loadIslandSchematic(File file, Location origin, PlayerPerk playerPerk) {
        new WEPasteSchematic(file, origin, playerPerk).runTask(uSkyBlock.getInstance());
    }

    public static void regenerate(Region region, Runnable onCompletion) {
        getAWEAdaptor().regenerate(region, onCompletion);
    }

    public static AWEAdaptor getAWEAdaptor() {
        if (adaptor == null) {
            if (!uSkyBlock.getInstance().getConfig().getBoolean("asyncworldedit.enabled", true)) {
                return NULL_ADAPTOR;
            }
            //Plugin fawe = getFAWE();
            Plugin fawe = null; // Disabled b/c 1.18.1 releases
            String className;
            if (fawe != null) {
                VersionUtil.Version version = VersionUtil.getVersion(fawe.getDescription().getVersion());
                className = "us.talabrek.ultimateskyblock.handler.asyncworldedit.FAWEAdaptor";
                try {
                    adaptor = (AWEAdaptor) Class.forName(className).getDeclaredConstructor().newInstance();
                    log(Level.INFO, "Hooked into FAWE version " + version);
                } catch (Exception ex) {
                    log(Level.WARNING, "Unable to locate FAWE adaptor for version " + version + ": " + ex);
                    adaptor = NULL_ADAPTOR;
                }
            } else {
                adaptor = NULL_ADAPTOR;
            }
        }
        return adaptor;
    }

    public static boolean isAWE() {
        return getAWEAdaptor() != NULL_ADAPTOR;
    }

    public static Plugin getFAWE() {
        return Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
    }

    public static final AWEAdaptor NULL_ADAPTOR = new AWEAdaptor() {
        @Override
        public void onEnable(uSkyBlock plugin) {

        }

        @Override
        public void registerCompletion(Player player) {

        }

        @Override
        public void loadIslandSchematic(File file, Location origin, PlayerPerk playerPerk) {
            WorldEditHandler.loadIslandSchematic(file, origin, playerPerk);
        }

        @Override
        public void onDisable(uSkyBlock plugin) {

        }

        @Override
        public EditSession createEditSession(World world, int maxBlocks) {
            return WorldEditHandler.createEditSession(world, maxBlocks);
        }

        @Override
        public void regenerate(final Region region, final Runnable onCompletion) {
            uSkyBlock.getInstance().sync(new Runnable() {
                @Override
                public void run() {
                    try {
                        final EditSession editSession = WorldEditHandler.createEditSession(region.getWorld(), (int) region.getVolume());
                        editSession.setReorderMode(EditSession.ReorderMode.MULTI_STAGE);
                        editSession.setSideEffectApplier(SideEffectSet.defaults());
                        editSession.getWorld().regenerate(region, editSession);
                        editSession.flushSession();
                    } finally {
                        onCompletion.run();
                    }
                }
            });
        }
    };
}
