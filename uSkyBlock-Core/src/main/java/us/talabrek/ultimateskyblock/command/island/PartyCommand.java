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
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.send;
import static us.talabrek.ultimateskyblock.util.Msg.sendPlayerOnly;

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
                    send(sender, tr("No pending invites."));
                } else {
                    String invites = String.join(", ", pendingInvitesAsNames);
                    send(sender, tr("Pending invites: <primary><invites></primary>", unparsed("invites", invites)));
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
                            send(sender, tr("<error>You don't have permission to uninvite players."));
                            return;
                        }
                        String playerName = args[0];
                        if (inviteHandler.uninvite(islandInfo, playerName)) {
                            send(sender, tr("Successfully withdrew invite for <primary><player></primary>.", unparsed("player", playerName)));
                        } else {
                            send(sender, tr("<error>No pending invite found for <player>", unparsed("player", playerName)));
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
            send(player, tr("<error>You do not have an island.</error> <muted>Use <cmd>/is create</cmd> to get one."));
            return true;
        }
        if (args.length == 0) {
            player.openInventory(menu.displayPartyGUI(player));
            return true;
        }
        return super.execute(sender, alias, data, args);
    }
}
