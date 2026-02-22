package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.async.Callback;
import us.talabrek.ultimateskyblock.api.model.BlockScore;
import us.talabrek.ultimateskyblock.api.model.IslandScore;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PatienceTester;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.miniToLegacy;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.util.Msg.sendLegacy;
import static net.kyori.adventure.text.minimessage.tag.resolver.Formatter.number;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.sendNoCommandAccess;

public class InfoCommand extends RequireIslandCommand {

    private final Logger logger;

    @Inject
    public InfoCommand(@NotNull uSkyBlock plugin, @NotNull Logger logger) {
        super(plugin, "info", "usb.island.info", "?island", marktr("check your or another's island info"));
        this.logger = logger;
        addFeaturePermission("usb.island.info.other", trLegacy("allows users to see others' island info"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (!Settings.island_useIslandLevel) {
            send(player, tr("<error>Island level has been disabled, contact an administrator."));
            return true;
        }
        if (PatienceTester.isRunning(player, "usb.island.info.active")) {
            return true;
        }
        if (player.hasMetadata("usb.island.info.active")) {
            send(player, tr("<error>Hold your horses!</error> <muted>You have to be patient."));
            return true;
        }
        if (args.length == 0 || (args.length == 1 && args[0].matches("\\d*"))) {
            if (!plugin.playerIsOnIsland(player)) {
                send(player, tr("<error>You must be on your island to use this command."));
                return true;
            }
            if (!island.isParty() && !pi.getHasIsland()) {
                send(player, tr("<error>You do not have an island!"));
            } else {
                getIslandInfo(player, player.getName(), alias, args.length == 1 ? Integer.parseInt(args[0], 10) : 1);
            }
            return true;
        } else if (args.length == 1 || (args.length == 2 && args[1].matches("\\d*"))) {
            if (hasPermission(player, "usb.island.info.other")) {
                getIslandInfo(player, args[0], alias, args.length == 2 ? Integer.parseInt(args[1], 10) : 1);
            } else {
                sendNoCommandAccess(player);
            }
            return true;
        }
        return false;
    }

    public boolean getIslandInfo(final Player player, final String islandPlayer, final String cmd, final int page) {
        PlayerInfo info = plugin.getPlayerInfo(islandPlayer);
        if (info == null || !info.getHasIsland()) {
            send(player, tr("<error>That player is invalid or does not have an island!"));
            return false;
        }
        final PlayerInfo playerInfo = islandPlayer.equals(player.getName()) ? plugin.getPlayerInfo(player) : plugin.getPlayerInfo(islandPlayer);
        final Callback<IslandScore> showInfo = new Callback<>() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    int maxPage = ((getState().getSize() - 1) / 10) + 1;
                    int currentPage = page;
                    if (currentPage < 1) {
                        currentPage = 1;
                    }
                    if (currentPage > maxPage) {
                        currentPage = maxPage;
                    }
                    send(player, tr("Blocks on <primary><player></primary>'s island (page <primary><page></primary> of <primary><max-page></primary>):",
                        unparsed("player", islandPlayer),
                        unparsed("page", String.valueOf(currentPage)),
                        unparsed("max-page", String.valueOf(maxPage))));
                    if (cmd.equalsIgnoreCase("info") && getState() != null) {
                        send(player, tr("Score Count Block"));
                        for (BlockScore score : getState().getTop((currentPage - 1) * 10, 10)) {
                            sendLegacy(player, score.getState().getColor() + miniToLegacy("<score:'00.00'>  <count:'#'> <block>",
                                number("score", score.getScore()),
                                number("count", score.getCount()),
                                legacyArg("block", ItemStackUtil.getBlockName(score.getBlockData()))));
                        }
                        send(player, tr("<secondary>Island level is <level:'###.##'>", number("level", getState().getScore())));
                    }
                }
                PatienceTester.stopRunning(player, "usb.island.info.active");
            }
        };
        try {
            PatienceTester.startRunning(player, "usb.island.info.active");
            plugin.calculateScoreAsync(player, playerInfo.locationForParty(), showInfo);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while calculating Island Level", e);
        }
        return true;
    }
}
