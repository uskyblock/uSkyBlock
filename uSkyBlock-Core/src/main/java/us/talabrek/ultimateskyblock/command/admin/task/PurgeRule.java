package us.talabrek.ultimateskyblock.command.admin.task;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

/**
 * The islands a purge takes: not ignored, below the purge level, and abandoned: every member offline and last played
 * before the cut-off. A member who never played counts as abandoned. The scan tests it, and the purge again right
 * before it deletes an island.
 */
public record PurgeRule(@NotNull Instant cutOff, double purgeLevel, @NotNull PlayerDB playerDB) {

    /** @param online the players online now, who are active whatever their last played says */
    public boolean matches(@NotNull IslandInfo island, @NotNull Set<UUID> online) {
        return !island.ignore() && island.getLevel() < purgeLevel && abandoned(island.getMemberUUIDs(), online);
    }

    private boolean abandoned(@NotNull Set<UUID> members, @NotNull Set<UUID> online) {
        for (UUID member : members) {
            // An online player's last played is their previous logout, or 0 in their first session.
            if (online.contains(member)) {
                return false;
            }
            OfflinePlayer player = playerDB.getOfflinePlayer(member);
            if (player == null || Instant.ofEpochMilli(player.getLastPlayed()).isAfter(cutOff)) {
                return false;
            }
        }
        return true;
    }

    public static @NotNull Set<UUID> online(@NotNull Server server) {
        return server.getOnlinePlayers().stream().map(Player::getUniqueId).collect(toSet());
    }
}
