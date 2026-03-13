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
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.api.event.AcceptEvent;
import us.talabrek.ultimateskyblock.api.event.InviteEvent;
import us.talabrek.ultimateskyblock.api.event.RejectEvent;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.message.Placeholder;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;

/**
 * Responsible for holding out-standing invites, and carrying out a transfer of invitation.
 */
@Singleton
public class InviteHandler implements Listener {
    private final Map<UUID, Invite> inviteMap = new HashMap<>();
    private final Map<String, Map<UUID, String>> waitingInvites = new HashMap<>();
    private final uSkyBlock plugin;
    private final Scheduler scheduler;
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public InviteHandler(@NotNull uSkyBlock plugin, @NotNull Scheduler scheduler, @NotNull RuntimeConfigs runtimeConfigs) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.runtimeConfigs = runtimeConfigs;
    }

    private synchronized void invite(Player player, final IslandInfo island, Player otherPlayer) {
        PlayerInfo oPi = plugin.getPlayerInfo(otherPlayer);
        Map<UUID, String> invites = waitingInvites.get(island.getName());
        if (invites == null) {
            invites = new HashMap<>();
        }
        if (island.getPartySize() + invites.size() >= island.getMaxPartySize()) {
            sendErrorTr(player, "Your island is full, or you have too many pending invites. You can't invite anyone else.");
            return;
        }
        if (oPi.getHasIsland()) {
            us.talabrek.ultimateskyblock.api.IslandInfo oIsland = plugin.getIslandInfo(oPi);
            if (oIsland.isParty() && !oIsland.isLeader(otherPlayer)) {
                sendErrorTr(player, "That player is already a member on another island.");
                sendTr(otherPlayer, "<player> tried to invite you, but you are already in a party.<newline><muted>Use <cmd>/is leave</cmd> to leave your current party.</muted>",
                    Placeholder.legacy("player", player.getDisplayName(), PRIMARY));
                return;
            }
        }
        final UUID uniqueId = otherPlayer.getUniqueId();
        invites.put(uniqueId, otherPlayer.getName());
        sendTr(player, "Invite sent to <player>.", Placeholder.legacy("player", otherPlayer.getDisplayName(), PRIMARY));
        sendTr(otherPlayer, "<player> invited you to join their island!",
            Placeholder.legacy("player", player.getDisplayName(), PRIMARY));
        sendTr(otherPlayer, "Use <cmd>/is accept</cmd> or <cmd>/is reject</cmd> to respond.", MUTED);
        sendErrorTr(otherPlayer, "Warning: Accepting will replace your current island.");
        Duration timeout = runtimeConfigs.current().party().inviteTimeout();
        BukkitTask timeoutTask = scheduler.async(() -> uninvite(island, uniqueId), timeout);
        final Invite invite = new Invite(island.getName(), player.getDisplayName(), timeoutTask);
        inviteMap.put(uniqueId, invite);
        waitingInvites.put(island.getName(), invites);
        island.sendMessageToIslandGroup(tr("<inviter> invited <invitee>.",
            Placeholder.legacy("inviter", player.getDisplayName(), PRIMARY),
            Placeholder.legacy("invitee", otherPlayer.getDisplayName(), PRIMARY)));
    }

    private synchronized boolean reject(Player player) {
        Invite invite = inviteMap.remove(player.getUniqueId());
        if (invite != null) {
            invite.timeoutTask().cancel();
            IslandInfo island = plugin.getIslandInfo(invite.islandName());
            if (island != null) {
                island.sendMessageToIslandGroup(tr("<player> rejected the invitation.",
                    Placeholder.legacy("player", player.getDisplayName(), PRIMARY)));
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
            sendErrorTr(player, "You can't use that command right now. Leave your current party first.");
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
                sendTr(player, "You joined an island. <muted>Use <cmd>/is party</cmd> to see the other members.</muted>");
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
        island.sendMessageToIslandGroup(tr("<player> joined your island group.",
            Placeholder.legacy("player", player.getDisplayName(), PRIMARY)));
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
            islandInfo.sendMessageToIslandGroup(tr("Invitation for <player> timed out or was cancelled.",
                Placeholder.legacy("player", invite.displayName(), PRIMARY)));
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                sendTr(player, "Invitation for <leader>'s island timed out or was cancelled.",
                    Placeholder.legacy("leader", islandInfo.getLeader(), PRIMARY));
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
                sendTr(e.getPlayer(), "You <success>accepted</success> the invitation to join an island.");
            } else {
                sendErrorTr(e.getPlayer(), "You haven't been invited.");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRejectEvent(RejectEvent e) {
        if (!e.isCancelled()) {
            if (reject(e.getPlayer())) {
                sendTr(e.getPlayer(), "You rejected the invitation to join an island.");
            } else {
                sendErrorTr(e.getPlayer(), "You haven't been invited.");
            }
        }
    }

    private record Invite(String islandName, String displayName, BukkitTask timeoutTask) {
    }
}
