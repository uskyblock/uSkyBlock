package us.talabrek.ultimateskyblock.api.model;

import org.jetbrains.annotations.NotNull;

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

    public IslandPartyMember getPartyMember(UUID uuid) {
        return partyMembers.get(uuid);
    }

    public void addPartyMember(UUID uuid, IslandPartyMember partyMember) {
        partyMembers.put(uuid, partyMember);
        setDirty(true);
    }

    public boolean removePartyMember(UUID uuid) {
        if (partyMembers.remove(uuid) != null) {
            setDirty(true);
            return true;
        }

        return false;
    }

    /**
     * Gets the size of the {@link Island} party. This includes the party leader, the size will always be 1 or higher.
     * @return Size of the island party.
     */
    public int getPartySize() {
        return getIsland().getIslandParty().getPartyMembers().size();
    }

    /**
     * Checks if the given {@link UUID} is a member of this {@link Island} party.
     * @param uuid Player's UUID
     * @return If island party member
     */
    public boolean isMember(@NotNull UUID uuid) {
        return partyMembers.containsKey(uuid);
    }

    /**
     * Checks if the given {@link UUID} is the leader of this {@link Island} party.
     * @param uuid Player's UUID
     * @return If island party member
     */
    public boolean isLeader(@NotNull UUID uuid) {
        if (!partyMembers.containsKey(uuid)) return false;
        return partyMembers.get(uuid).getRole().equals(IslandPartyMember.Role.LEADER);
    }
}
