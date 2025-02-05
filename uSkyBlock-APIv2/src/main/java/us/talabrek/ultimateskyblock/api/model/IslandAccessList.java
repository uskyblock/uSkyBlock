package us.talabrek.ultimateskyblock.api.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
}
