package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.IslandRank;
import us.talabrek.ultimateskyblock.api.async.Callback;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static net.kyori.adventure.text.minimessage.tag.resolver.Formatter.number;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.message.Msg.send;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendNoCommandAccess;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;

public class LevelCommand extends RequireIslandCommand {

    @Inject
    public LevelCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "level", "usb.island.level", "?island", marktr("check your or another's island level"));
        addFeaturePermission("usb.island.level.other", trLegacy("allows users to query others' levels"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (!Settings.island_useIslandLevel) {
            sendErrorTr(player, "Island level has been disabled, contact an administrator.");
            return true;
        }
        if (args.length == 0) {
            if (!plugin.playerIsOnIsland(player)) {
                sendErrorTr(player, "You must be on your island to use this command.");
                return true;
            }
            if (!island.isParty() && !pi.getHasIsland()) {
                sendErrorTr(player, "You do not have an island!");
            } else {
                getIslandLevel(player, player.getName(), alias);
            }
            return true;
        } else if (args.length == 1) {
            if (hasPermission(player, "usb.island.level.other")) {
                getIslandLevel(player, args[0], alias);
            } else {
                sendNoCommandAccess(player);
            }
            return true;
        }
        return false;
    }

    public boolean getIslandLevel(final Player player, final String islandPlayer, final String cmd) {
        final PlayerInfo info = plugin.getPlayerInfo(islandPlayer);
        if (info == null || !info.getHasIsland()) {
            sendErrorTr(player, "That player is invalid or does not have an island!");
            return false;
        }
        final us.talabrek.ultimateskyblock.api.IslandInfo islandInfo = plugin.getIslandInfo(info);
        if (islandInfo == null || islandInfo.getIslandLocation() == null) {
            sendErrorTr(player, "That player is invalid or does not have an island!");
            return false;
        }
        final boolean shouldRecalculate = player.getName().equals(info.getPlayerName()) || player.hasPermission("usb.admin.island");
        final Runnable showInfo = () -> {
            if (player != null && player.isOnline() && info != null) {
                sendTr(player, "Information about <player>'s island:", unparsed("player", islandPlayer, PRIMARY));
                if (cmd.equalsIgnoreCase("level")) {
                    IslandRank rank = plugin.getIslandLogic().getRank(info.locationForParty());
                    if (rank != null) {
                        send(player,
                            tr("Island level is <score:'###.##'>", SECONDARY, number("score", rank.getScore())),
                            tr("Rank is <rank>", PRIMARY, unparsed("rank", String.valueOf(rank.getRank()))));
                    } else {
                        sendErrorTr(player, "Could not locate rank of <player>", unparsed("player", islandPlayer));
                    }
                }
            }
        };
        if (shouldRecalculate) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.calculateScoreAsync(player, info.locationForParty(), new Callback<us.talabrek.ultimateskyblock.api.model.IslandScore>() {
                @Override
                public void run() {
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, showInfo, 10L);
                }
            }), 1L);
        } else {
            showInfo.run();
        }
        return true;
    }
}
