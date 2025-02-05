package us.talabrek.ultimateskyblock.api.model;

import java.util.UUID;

public class IslandAccess extends Model {
    protected final UUID playerUuid;
    protected final AccessType accessType;

    public IslandAccess(UUID playerUuid, AccessType accessType) {
        this.playerUuid = playerUuid;
        this.accessType = accessType;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public enum AccessType {
        BANNED,
        TRUSTED
    }
}
