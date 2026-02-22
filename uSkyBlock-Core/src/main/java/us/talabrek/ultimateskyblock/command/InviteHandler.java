package us.talabrek.ultimateskyblock.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.event.AcceptEvent;
import us.talabrek.ultimateskyblock.api.event.InviteEvent;
import us.talabrek.ultimateskyblock.api.event.RejectEvent;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.util.Msg.send;

/**
 * Responsible for holding out-standing invites, and carrying out a transfer of invitation.
 */
@Singleton
public class InviteHandler implements Listener {
    private final Map<UUID, Invite> inviteMap = new HashMap<>();
    private final Map<String, Map<UUID, String>> waitingInvites = new HashMap<>();
    private final uSkyBlock plugin;
    private final Scheduler scheduler;

    @Inject
    public InviteHandler(@NotNull uSkyBlock plugin, @NotNull Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    private synchronized void invite(Player player, final IslandInfo island, Player otherPlayer) {
        PlayerInfo oPi = plugin.getPlayerInfo(otherPlayer);
        Map<UUID, String> invites = waitingInvites.get(island.getName());
        if (invites == null) {
            invites = new HashMap<>();
        }
        if (island.getPartySize() + invites.size() >= island.getMaxPartySize()) {
            send(player, tr("<error>Your island is full, or you have too many pending invites. You can't invite anyone else."));
            return;
        }
        if (oPi.getHasIsland()) {
            us.talabrek.ultimateskyblock.api.IslandInfo oIsland = plugin.getIslandInfo(oPi);
            if (oIsland.isParty() && !oIsland.isLeader(otherPlayer)) {
                send(player, tr("<error>That player is already a member on another island."));
                send(otherPlayer, tr("<primary><player></primary> tried to invite you, but you are already in a party.<newline><muted>Use <cmd>/is leave</cmd> to leave your current party.</muted>",
                    legacyArg("player", player.getDisplayName())));
                return;
            }
        }
        final UUID uniqueId = otherPlayer.getUniqueId();
        invites.put(uniqueId, otherPlayer.getName());
        send(player, tr("Invite sent to <primary><player></primary>.", legacyArg("player", otherPlayer.getDisplayName())));
        send(otherPlayer, tr("<primary><player></primary> invited you to join their island!",
                legacyArg("player", player.getDisplayName())),
            tr("<muted>Use <cmd>/is accept</cmd> or <cmd>/is reject</cmd> to respond."),
            tr("<error>Warning: Accepting will replace your current island."));
        Duration timeout = Duration.ofSeconds(plugin.getConfig().getInt("options.party.invite-timeout", 30));
        BukkitTask timeoutTask = scheduler.async(() -> uninvite(island, uniqueId), timeout);
        final Invite invite = new Invite(island.getName(), player.getDisplayName(), timeoutTask);
        inviteMap.put(uniqueId, invite);
        waitingInvites.put(island.getName(), invites);
        island.sendMessageToIslandGroup(tr("<primary><inviter></primary> invited <primary><invitee></primary>.",
            legacyArg("inviter", player.getDisplayName()),
            legacyArg("invitee", otherPlayer.getDisplayName())));
    }

    private synchronized boolean reject(Player player) {
        Invite invite = inviteMap.remove(player.getUniqueId());
        if (invite != null) {
            invite.timeoutTask().cancel();
            IslandInfo island = plugin.getIslandInfo(invite.islandName());
            if (island != null) {
                island.sendMessageToIslandGroup(tr("<primary><player></primary> rejected the invitation.",
                    legacyArg("player", player.getDisplayName())));
            }
            if (waitingInvites.containsKey(invite.islandName())) {
                waitingInvites.get(invite.islandName()).remove(player.getUniqueId());
            }
            return true;
        }
        return false;
    }

    private synchronized boolean accept(final Player player) {
        UUID uuid = player.getUniqueId();
        us.talabrek.ultimateskyblock.api.IslandInfo oldIsland = plugin.getIslandInfo(player);
        if (oldIsland != null && oldIsland.isParty()) {
            send(player, tr("<error>You can't use that command right now. Leave your current party first."));
            return false;
        }
        Invite invite = inviteMap.remove(uuid);
        if (invite != null) {
            invite.timeoutTask().cancel();
            PlayerInfo pi = plugin.getPlayerInfo(player);
            final IslandInfo island = plugin.getIslandInfo(invite.islandName());
            boolean deleteOldIsland = false;
            if (pi.getHasIsland() && pi.getIslandLocation() != null) {
                String islandName = WorldGuardHandler.getIslandNameAt(pi.getIslandLocation());
                deleteOldIsland = !island.getName().equals(islandName);
            }
            Map<UUID, String> uuids = waitingInvites.get(invite.islandName());
            if (uuids != null) {
                uuids.remove(uuid);
            }
            Runnable joinIsland = () -> {
                send(player, tr("You joined an island. <muted>Use <cmd>/is party</cmd> to see the other members.</muted>"));
                addPlayerToParty(player, island);
                plugin.getTeleportLogic().homeTeleport(player, true);
                plugin.clearPlayerInventory(player);
            };
            if (deleteOldIsland) {
                plugin.deletePlayerIsland(player.getName(), joinIsland);
            } else {
                joinIsland.run();
            }
            return true;
        }
        return false;
    }

    public synchronized Set<UUID> getPendingInvites(IslandInfo island) {
        return waitingInvites.containsKey(island.getName()) ? waitingInvites.get(island.getName()).keySet() : null;
    }

    public synchronized Collection<String> getPendingInvitesAsNames(IslandInfo island) {
        return waitingInvites.containsKey(island.getName()) ? waitingInvites.get(island.getName()).values() : null;
    }

    public boolean addPlayerToParty(final Player player, final IslandInfo island) {
        PlayerInfo playerInfo = plugin.getPlayerInfo(player);
        island.addMember(playerInfo);
        playerInfo.save();
        island.sendMessageToIslandGroup(tr("<primary><player></primary> joined your island group.",
            legacyArg("player", player.getDisplayName())));
        return true;
    }

    public synchronized boolean uninvite(IslandInfo islandInfo, String playerName) {
        if (Bukkit.isPrimaryThread()) {
            throw new UnsupportedOperationException("This method cannot be called in the primary thread!");
        }

        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
        if (offlinePlayer != null) {
            UUID uuid = offlinePlayer.getUniqueId();
            return uninvite(islandInfo, uuid);
        }
        return false;
    }

    private synchronized boolean uninvite(IslandInfo islandInfo, UUID uuid) {
        Set<UUID> invites = getPendingInvites(islandInfo);
        if (invites != null && invites.contains(uuid)) {
            Invite invite = inviteMap.remove(uuid);
            invites.remove(uuid);
            if (invite != null) {
                invite.timeoutTask().cancel();
            }
            islandInfo.sendMessageToIslandGroup(tr("Invitation for <primary><player></primary> timed out or was cancelled.",
                legacyArg("player", invite.displayName())));
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                send(player, tr("Invitation for <primary><leader></primary>'s island timed out or was cancelled.",
                    legacyArg("leader", islandInfo.getLeader())));
            }
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInviteEvent(InviteEvent e) {
        if (!e.isCancelled()) {
            invite(e.getPlayer(), (IslandInfo) e.getIslandInfo(), e.getGuest());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAcceptEvent(AcceptEvent e) {
        if (!e.isCancelled()) {
            if (accept(e.getPlayer())) {
                send(e.getPlayer(), tr("You <success>accepted</success> the invitation to join an island."));
            } else {
                send(e.getPlayer(), tr("<error>You haven't been invited."));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRejectEvent(RejectEvent e) {
        if (!e.isCancelled()) {
            if (reject(e.getPlayer())) {
                send(e.getPlayer(), tr("You rejected the invitation to join an island."));
            } else {
                send(e.getPlayer(), tr("<error>You haven't been invited."));
            }
        }
    }

    private record Invite(String islandName, String displayName, BukkitTask timeoutTask) {
    }
}
