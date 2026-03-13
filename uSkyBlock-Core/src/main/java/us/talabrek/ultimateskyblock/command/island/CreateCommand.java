package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.api.event.CreateIslandEvent;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.time.Duration;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;

public class CreateCommand extends RequirePlayerCommand {
    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public CreateCommand(@NotNull uSkyBlock plugin, @NotNull RuntimeConfigs runtimeConfigs) {
        super("create|c", "usb.island.create", "?schematic", marktr("create an island"));
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
        addFeaturePermission("usb.exempt.cooldown.create", trLegacy("exempt player from create-cooldown"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, Map<String, Object> data, String... args) {
        PlayerInfo pi = plugin.getPlayerInfo(player);
        Duration cooldown = plugin.getCooldownHandler().getCooldown(player, "restart");
        if (!pi.getHasIsland() && cooldown.isZero()) {
            String cSchem = args != null && args.length > 0 ? args[0] : runtimeConfigs.current().island().defaultScheme();
            plugin.getServer().getPluginManager().callEvent(new CreateIslandEvent(player, cSchem));
        } else if (pi.getHasIsland()) {
            IslandInfo island = plugin.getIslandInfo(pi);
            if (island.isLeader(player)) {
                sendErrorTr(player, "You already have an island. <muted>If you want a fresh island, use <cmd>/is restart</cmd>.");
            } else {
                sendErrorTr(player, "You are already a member of an island. <muted>To start your own, first use <cmd>/is leave</cmd>.");
            }
        } else {
            // I18N: <seconds> is a localized number tag. Tag arguments use DecimalFormat patterns; keep tag name "seconds".
            sendTr(player, "You can create a new island in <seconds> seconds.",
                number("seconds", cooldown.toSeconds(), PRIMARY));
        }
        return true;
    }
}
