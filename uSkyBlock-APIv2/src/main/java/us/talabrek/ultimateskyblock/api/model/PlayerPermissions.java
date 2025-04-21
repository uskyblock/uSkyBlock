package us.talabrek.ultimateskyblock.api.model;

import java.util.HashSet;
import java.util.Set;

public class PlayerPermissions extends Model {
    protected final Player player;
    protected Set<PlayerPermission> permissions = new HashSet<>();

    public PlayerPermissions(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Set<PlayerPermission> getPermissions() {
        return Set.copyOf(permissions);
    }

    public void addPermission(PlayerPermission permission) {
        permissions.add(permission);
        setDirty(true);
    }

    public void clearPermissions() {
        permissions.clear();
        setDirty(true);
    }
}
