package us.talabrek.ultimateskyblock.api.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class IslandAccessList extends Model {
    protected final Island island;
    protected ConcurrentMap<UUID, IslandAccess> acl = new ConcurrentHashMap<>();

    public IslandAccessList(Island island) {
        this.island = island;
    }

    public Island getIsland() {
        return island;
    }

    public Map<UUID, IslandAccess> getAcl() {
        return Map.copyOf(acl);
    }

    public void addIslandAccess(IslandAccess islandAccess) {
        acl.put(islandAccess.getPlayerUuid(), islandAccess);
        setDirty(true);
    }

    public void removeIslandAccess(UUID playerUuid) {
        acl.remove(playerUuid);
        setDirty(true);
    }

    /**
     * Returns an immutable copy of all players with the given {@link IslandAccess.AccessType} on this {@link Island}.
     * @param type AccessType to filter.
     * @return An unmodifiable map of users with the given access type.
     */
    public Map<UUID, IslandAccess> getAccessByType(IslandAccess.AccessType type) {
        return Map.copyOf(acl.entrySet().stream()
            .filter(entry -> entry.getValue().getAccessType() == type)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * Returns an immutable copy of all banned users on this {@link Island}.
     * @return An unmodifiable map of banned users.
     */
    public Map<UUID, IslandAccess> getBanned() {
        return getAccessByType(IslandAccess.AccessType.BANNED);
    }

    /**
     * Returns an immutable copy of all trusted users on this {@link Island}.
     * @return An unmodifiable map of trusted users.
     */
    public Map<UUID, IslandAccess> getTrusted() {
        return getAccessByType(IslandAccess.AccessType.TRUSTED);
    }

    public boolean isBanned(UUID playerUuid) {
        IslandAccess islandAccess = acl.get(playerUuid);
        if (islandAccess == null) return false;
        return islandAccess.getAccessType().equals(IslandAccess.AccessType.BANNED);
    }

    public boolean isTrusted(UUID playerUuid) {
        IslandAccess islandAccess = acl.get(playerUuid);
        if (islandAccess == null) return false;
        return islandAccess.getAccessType().equals(IslandAccess.AccessType.TRUSTED);
    }
}
