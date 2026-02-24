package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.util.Msg.MUTED;
import static us.talabrek.ultimateskyblock.util.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

public class WarpCommand extends RequirePlayerCommand {

    private final uSkyBlock plugin;

    @Inject
    public WarpCommand(@NotNull uSkyBlock plugin) {
        super("warp|w", "usb.island.warp", "island", marktr("warp to another player's island"));
        this.plugin = plugin;
    }

    @Override
    protected boolean doExecute(String alias, Player player, Map<String, Object> data, String... args) {
        if (args.length == 0) {
            IslandInfo island = plugin.getIslandInfo(player);
            if (island != null && hasPermission(player, "usb.island.setwarp")) {
                if (island.hasWarp()) {
                    sendTr(player, "Your incoming warp is active, players may warp to your island.", SECONDARY);
                } else {
                    sendErrorTr(player, "Your incoming warp is inactive, players may not warp to your island.");
                }
                sendTr(player, "Set incoming warp to your current location using <cmd>/is setwarp</cmd>.", MUTED);
                sendTr(player, "Toggle your warp on or off using <cmd>/is togglewarp</cmd>.", MUTED);
            } else {
                sendErrorTr(player, "You do not have permission to create a warp on your island!");
            }
            if (hasPermission(player, "usb.island.warp")) {
                sendTr(player, "Warp to another player's island using <cmd>/is warp [player]</cmd>.", MUTED);
            } else {
                sendErrorTr(player, "You do not have permission to warp to other islands!");
            }
            return true;
        } else if (args.length == 1) {
            if (hasPermission(player, "usb.island.warp")) {
                PlayerInfo senderPlayerInfo = plugin.getPlayerInfo(player);
                if (senderPlayerInfo.isIslandGenerating()) {
                    sendErrorTr(player, "Your island is currently generating, so you cannot warp to other players' islands right now.");
                    return true;
                }

                PlayerInfo targetPlayerInfo = plugin.getPlayerInfo(args[0]);
                if (targetPlayerInfo == null || !targetPlayerInfo.getHasIsland()) {
                    sendErrorTr(player, "That player does not exist!");
                    return true;
                }
                IslandInfo island = plugin.getIslandInfo(targetPlayerInfo);
                if (island == null || (!island.hasWarp() && !island.isTrusted(player))) {
                    sendErrorTr(player, "That player does not have an active warp.");
                    return true;
                }
                if (targetPlayerInfo.isIslandGenerating()) {
                    sendErrorTr(player, "That player's island is currently generating, so you cannot warp to it right now.");
                    return true;
                }
                if (!island.isBanned(player)) {
                    if (plugin.getConfig().getBoolean("options.protection.visitors.warn-on-warp", true)) {
                        island.sendMessageToOnlineMembers(trLegacy("<error>Warning:</error> <primary><player></primary> is warping to your island.",
                            legacyArg("player", player.getDisplayName())));
                    }
                    plugin.getTeleportLogic().warpTeleport(player, targetPlayerInfo, false);
                } else {
                    sendErrorTr(player, "That player has forbidden you from warping to their island.");
                }
            } else {
                sendErrorTr(player, "You do not have permission to warp to other islands!");
            }
            return true;
        }
        return false;
    }
}
