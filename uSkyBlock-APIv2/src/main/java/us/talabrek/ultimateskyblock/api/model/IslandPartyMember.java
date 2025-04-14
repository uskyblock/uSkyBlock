package us.talabrek.ultimateskyblock.api.model;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

public class IslandPartyMember extends Model {
    protected final UUID uuid;
    protected Role role;
    protected Set<String> permissions = new ConcurrentSkipListSet<>();

    public IslandPartyMember(UUID uuid, Role role, Set<String> permissions) {
        this.uuid = uuid;
        this.role = role;
        this.permissions = new ConcurrentSkipListSet<>(permissions);
    }

    public IslandPartyMember(UUID uuid) {
        this.uuid = uuid;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
        setDirty(true);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Set<String> getPermissions() {
        return Set.copyOf(permissions);
    }

    public boolean hasPermission(String node) {
        return permissions.contains(node);
    }

    public void setPermission(String node) {
        permissions.add(node);
        setDirty(true);
    }

    public void removePermission(String node) {
        permissions.remove(node);
        setDirty(true);
    }

    public enum Role {
        MEMBER,
        LEADER
    }
}
