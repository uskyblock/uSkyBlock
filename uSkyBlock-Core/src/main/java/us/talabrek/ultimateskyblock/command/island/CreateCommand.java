package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.api.event.CreateIslandEvent;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.time.Duration;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

public class CreateCommand extends RequirePlayerCommand {
    private final uSkyBlock plugin;

    @Inject
    public CreateCommand(@NotNull uSkyBlock plugin) {
        super("create|c", "usb.island.create", "?schematic", marktr("create an island"));
        this.plugin = plugin;
        addFeaturePermission("usb.exempt.cooldown.create", trLegacy("exempt player from create-cooldown"));
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
                send(player, tr("<error>Island found.</error> <muted>You already have an island. If you want a fresh island, use <cmd>/is restart</cmd>."));
            } else {
                send(player, tr("<error>Island found.</error> <muted>You are already a member of an island. To start your own, first use <cmd>/is leave</cmd>."));
            }
        } else {
            send(player, tr("You can create a new island in <primary><seconds></primary> seconds.",
                unparsed("seconds", String.valueOf(cooldown.toSeconds()))));
        }
        return true;
    }
}
