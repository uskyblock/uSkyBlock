package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.api.event.CreateIslandEvent;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.time.Duration;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

public class CreateCommand extends RequirePlayerCommand {
    private final uSkyBlock plugin;

    @Inject
    public CreateCommand(@NotNull uSkyBlock plugin) {
        super("create|c", "usb.island.create", "?schematic", marktr("create an island"));
        this.plugin = plugin;
        addFeaturePermission("usb.exempt.cooldown.create", tr("exempt player from create-cooldown"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, Map<String, Object> data, String... args) {
        PlayerInfo pi = plugin.getPlayerInfo(player);
        Duration cooldown = plugin.getCooldownHandler().getCooldown(player, "restart");
        if (!pi.getHasIsland() && cooldown.isZero()) {
            String cSchem = args != null && args.length > 0 ? args[0] : Settings.island_schematicName;
            plugin.getServer().getPluginManager().callEvent(new CreateIslandEvent(player, cSchem));
        } else if (pi.getHasIsland()) {
            IslandInfo island = plugin.getIslandInfo(pi);
            if (island.isLeader(player)) {
                player.sendMessage(tr("\u00a74Island found!" +
                    "\u00a7e You already have an island. If you want a fresh island, type" +
                    "\u00a7b /is restart\u00a7e to get one"));
            } else {
                player.sendMessage(tr("\u00a74Island found!" +
                    "\u00a7e You are already a member of an island. To start your own, first" +
                    "\u00a7b /is leave"));
            }
        } else {
            player.sendMessage(tr("\u00a7eYou can create a new island in {0,number,#} seconds.", cooldown.toSeconds()));
        }
        return true;
    }
}
