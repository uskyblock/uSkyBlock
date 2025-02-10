package us.talabrek.ultimateskyblock.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.po.I18nUtil;
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

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

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
            player.sendMessage(tr("\u00a74Your island is full, or you have too many pending invites. You can't invite anyone else."));
            return;
        }
        if (oPi.getHasIsland()) {
            us.talabrek.ultimateskyblock.api.IslandInfo oIsland = plugin.getIslandInfo(oPi);
            if (oIsland.isParty() && !oIsland.isLeader(otherPlayer)) {
                player.sendMessage(tr("§4That player is already member on another island. "));
                otherPlayer.sendMessage(tr("§e{0}§e tried to invite you, but you are already in a party." +
                    "To leave your current party, use: /island leave.", player.getDisplayName()));
                return;
            }
        }
        final UUID uniqueId = otherPlayer.getUniqueId();
        invites.put(uniqueId, otherPlayer.getName());
        player.sendMessage(tr("\u00a7aInvite sent to {0}", otherPlayer.getDisplayName()));
        otherPlayer.sendMessage(tr("{0}\u00a7e has invited you to join their island!", player.getDisplayName()),
            tr("\u00a7f/island [accept/reject]\u00a7e to accept or reject the invite."),
            tr("\u00a74WARNING: You will lose your current island if you accept!"));
        Duration timeout = Duration.ofSeconds(plugin.getConfig().getInt("options.party.invite-timeout", 30));
        BukkitTask timeoutTask = scheduler.async(() -> uninvite(island, uniqueId), timeout);
        final Invite invite = new Invite(island.getName(), player.getDisplayName(), timeoutTask);
        inviteMap.put(uniqueId, invite);
        waitingInvites.put(island.getName(), invites);
        island.sendMessageToIslandGroup(true, I18nUtil.marktr("{0}\u00a7d invited {1}"), player.getDisplayName(), otherPlayer.getDisplayName());
    }

    private synchronized boolean reject(Player player) {
        Invite invite = inviteMap.remove(player.getUniqueId());
        if (invite != null) {
            invite.timeoutTask().cancel();
            IslandInfo island = plugin.getIslandInfo(invite.islandName());
            if (island != null) {
                island.sendMessageToIslandGroup(true, marktr("{0}\u00a7e has rejected the invitation."), player.getDisplayName());
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
            player.sendMessage(tr("\u00a74You can't use that command right now. Leave your current party first."));
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
                player.sendMessage(tr("\u00a7aYou have joined an island! Use /island party to see the other members."));
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
        island.sendMessageToIslandGroup(true, marktr("\u00a7b{0}\u00a7d has joined your island group."), player.getDisplayName());
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
            islandInfo.sendMessageToIslandGroup(true, marktr("\u00a7eInvitation for {0}\u00a7e has timedout or been cancelled."), invite.displayName());
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(tr("\u00a7eInvitation for {0}''s island has timedout or been cancelled.", islandInfo.getLeader()));
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
                e.getPlayer().sendMessage(I18nUtil.tr("\u00a7eYou have accepted the invitation to join an island."));
            } else {
                e.getPlayer().sendMessage(I18nUtil.tr("\u00a74You haven't been invited."));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRejectEvent(RejectEvent e) {
        if (!e.isCancelled()) {
            if (reject(e.getPlayer())) {
                e.getPlayer().sendMessage(I18nUtil.tr("\u00a7eYou have rejected the invitation to join an island."));
            } else {
                e.getPlayer().sendMessage(I18nUtil.tr("\u00a74You haven't been invited."));
            }
        }
    }

    private record Invite(String islandName, String displayName, BukkitTask timeoutTask) {
    }
}
