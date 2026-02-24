package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.command.island.RequirePlayerCommand;
import us.talabrek.ultimateskyblock.handler.WorldEditHandler;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

/**
 * Some hooks into the WG Handler
 */
public class WGCommand extends CompositeCommand {
    private final uSkyBlock plugin;

    @Inject
    public WGCommand(@NotNull final uSkyBlock plugin) {
        super("wg", "usb.admin.wg", marktr("various WorldGuard utilities"));
        this.plugin = plugin;
        add(new RequirePlayerCommand("refresh", null, marktr("refreshes the chunks around the player")) {
            @Override
            protected boolean doExecute(String alias, Player player, Map<String, Object> data, String... args) {
                WorldEditHandler.refreshRegion(player.getLocation());
                sendTr(player, "Resending chunks to the client.");
                return true;
            }
        });
        add(new RequirePlayerCommand("load", null, marktr("load the region chunks")) {
            @Override
            protected boolean doExecute(String alias, Player player, Map<String, Object> data, String... args) {
                WorldEditHandler.loadRegion(player.getLocation());
                sendTr(player, "Loading chunks at <primary><location></primary>.",
                    unparsed("location", LocationUtil.asString(player.getLocation())));
                return true;
            }
        });
        add(new RequirePlayerCommand("unload", null, marktr("load the region chunks")) {
            @Override
            protected boolean doExecute(String alias, Player player, Map<String, Object> data, String... args) {
                LocationUtil.loadChunkAt(player.getLocation());
                sendTr(player, "Unloading chunks at <primary><location></primary>.",
                    unparsed("location", LocationUtil.asString(player.getLocation())));
                return true;
            }
        });
        add(new RequirePlayerCommand("update", null, marktr("update the WG regions")) {
            @Override
            protected boolean doExecute(String alias, Player player, Map<String, Object> data, String... args) {
                String island = WorldGuardHandler.getIslandNameAt(player.getLocation());
                if (island != null) {
                    IslandInfo islandInfo = plugin.getIslandInfo(island);
                    if (islandInfo != null) {
                        WorldGuardHandler.updateRegion(islandInfo);
                        sendTr(player, "Island WorldGuard regions updated for <primary><island></primary>.",
                            unparsed("island", island));
                    } else {
                        sendErrorTr(player, "No island found at your location.");
                    }
                }
                return true;
            }
        });
        add(new RequirePlayerCommand("chunk", null, marktr("refreshes the chunk around the player")) {
            @Override
            protected boolean doExecute(String alias, Player player, Map<String, Object> data, String... args) {
                World world = player.getWorld();
                world.refreshChunk(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
                sendTr(player, "Resending chunks to the client.");
                return true;
            }
        });
    }
}
