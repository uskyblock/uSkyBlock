package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.async.Callback;
import us.talabrek.ultimateskyblock.api.model.BlockScore;
import us.talabrek.ultimateskyblock.api.model.IslandScore;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PatienceTester;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.message.Msg.send;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendNoCommandAccess;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.*;

public class InfoCommand extends RequireIslandCommand {

    private final Logger logger;
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public InfoCommand(@NotNull uSkyBlock plugin, @NotNull Logger logger, @NotNull RuntimeConfigs runtimeConfigs) {
        super(plugin, "info", "usb.island.info", "?island", marktr("check your or another's island info"));
        this.logger = logger;
        this.runtimeConfigs = runtimeConfigs;
        addFeaturePermission("usb.island.info.other", trLegacy("allows users to see others' island info"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (!runtimeConfigs.current().island().useIslandLevel()) {
            sendErrorTr(player, "Island level has been disabled, contact an administrator.");
            return true;
        }
        if (PatienceTester.isRunning(player, "usb.island.info.active")) {
            return true;
        }
        if (player.hasMetadata("usb.island.info.active")) {
            sendErrorTr(player, "Hold your horses! <muted>You have to be patient.");
            return true;
        }
        if (args.length == 0 || (args.length == 1 && args[0].matches("\\d*"))) {
            if (!plugin.playerIsOnIsland(player)) {
                sendErrorTr(player, "You must be on your island to use this command.");
                return true;
            }
            if (!island.isParty() && !pi.getHasIsland()) {
                sendErrorTr(player, "You do not have an island!");
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
            sendErrorTr(player, "That player is invalid or does not have an island!");
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
                    sendTr(player, "Blocks on <player>'s island (page <page> of <max-page>):",
                        unparsed("player", islandPlayer, PRIMARY),
                        unparsed("page", String.valueOf(currentPage), PRIMARY),
                        unparsed("max-page", String.valueOf(maxPage), PRIMARY));
                    if (cmd.equalsIgnoreCase("info") && getState() != null) {
                        sendTr(player, "Score Count Block");
                        for (BlockScore score : getState().getTop((currentPage - 1) * 10, 10)) {
                            send(player, parseMini("<score:'#,##0.00'>  <count:'#,##0'> <block>",
                                number("score", score.getScore()),
                                number("count", score.getCount()),
                                component("block", ItemStackUtil.getBlockName(score.getBlockData())))
                                .applyFallbackStyle(styleFromBlockScoreState(score.getState())));
                        }
                        sendTr(player, "Island level is <level:'#,##0'>", SECONDARY, number("level", getState().getScore()));
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

    private static @NotNull Style styleFromBlockScoreState(@NotNull BlockScore.State state) {
        return switch (state) {
            case NORMAL -> Style.style(NamedTextColor.AQUA);
            case DIMINISHING -> Style.style(NamedTextColor.YELLOW);
            case LIMIT -> Style.style(NamedTextColor.RED);
            case NEGATIVE -> Style.style(NamedTextColor.DARK_PURPLE);
        };
    }
}
