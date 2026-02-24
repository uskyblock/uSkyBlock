package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.command.InviteHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.menu.SkyBlockMenu;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Collection;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.util.Msg.send;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendPlayerOnly;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

public class PartyCommand extends CompositeCommand {
    private final uSkyBlock plugin;
    private final SkyBlockMenu menu;

    @Inject
    public PartyCommand(@NotNull uSkyBlock plugin, @NotNull SkyBlockMenu menu, @NotNull InviteHandler inviteHandler) {
        super("party", null, marktr("show party information"));
        this.plugin = plugin;
        this.menu = menu;
        add(new AbstractCommand("info", "usb.party.info", marktr("shows information about your party")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                send(sender, plugin.getIslandInfo((Player) sender).asComponentLines());
                return true;
            }
        });
        add(new AbstractCommand("invites", "usb.party.invites", marktr("show pending invites")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                IslandInfo islandInfo = plugin.getIslandInfo((Player) sender);
                Collection<String> pendingInvitesAsNames = inviteHandler.getPendingInvitesAsNames(islandInfo);
                if (pendingInvitesAsNames == null || pendingInvitesAsNames.isEmpty()) {
                    sendTr(sender, "No pending invites.");
                } else {
                    String invites = String.join(", ", pendingInvitesAsNames);
                    sendTr(sender, "Pending invites: <invites>", unparsed("invites", invites, PRIMARY));
                }
                return true;
            }
        });
        add(new AbstractCommand("uninvite", "usb.party.uninvite", "player", marktr("withdraw an invite")) {
            @Override
            public boolean execute(final CommandSender sender, String alias, Map<String, Object> data, final String... args) {
                if (args.length == 1 && sender instanceof Player) {
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                        IslandInfo islandInfo = plugin.getIslandInfo((Player) sender);
                        if (!islandInfo.isLeader((Player) sender) || !islandInfo.hasPerm(sender.getName(), "canInviteOthers")) {
                            sendErrorTr(sender, "You don't have permission to uninvite players.");
                            return;
                        }
                        String playerName = args[0];
                        if (inviteHandler.uninvite(islandInfo, playerName)) {
                            sendTr(sender, "Successfully withdrew invite for <player>.", unparsed("player", playerName, PRIMARY));
                        } else {
                            sendErrorTr(sender, "No pending invite found for <player>", unparsed("player", playerName));
                        }
                    });
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        if (!(sender instanceof Player player)) {
            sendPlayerOnly(sender);
            return false;
        }
        PlayerInfo playerInfo = plugin.getPlayerInfo(player);
        if (playerInfo == null || !playerInfo.getHasIsland()) {
            sendErrorTr(player, "You do not have an island. <muted>Use <cmd>/is create</cmd> to get one.");
            return true;
        }
        if (args.length == 0) {
            player.openInventory(menu.displayPartyGUI(player));
            return true;
        }
        return super.execute(sender, alias, data, args);
    }
}
