package us.talabrek.ultimateskyblock.api.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class IslandParty extends Model {
    protected final Island island;
    protected ConcurrentMap<UUID, IslandPartyMember> partyMembers = new ConcurrentHashMap<>();

    public IslandParty(Island island) {
        this.island = island;
    }

    public Island getIsland() {
        return island;
    }

    public Map<UUID, IslandPartyMember> getPartyMembers() {
        return Map.copyOf(partyMembers);
    }

    public void addPartyMember(UUID uuid, IslandPartyMember partyMember) {
        partyMembers.put(uuid, partyMember);
        setDirty(true);
    }

    public boolean removePartyMember(UUID uuid, IslandPartyMember partyMember) {
        if (partyMembers.remove(uuid, partyMember)) {
            setDirty(true);
            return true;
        }

        return false;
    }
}
