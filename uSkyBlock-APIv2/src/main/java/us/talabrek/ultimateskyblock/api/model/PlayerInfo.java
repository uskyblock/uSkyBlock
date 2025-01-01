package us.talabrek.ultimateskyblock.api.model;

import java.util.UUID;

public class PlayerInfo extends Model {
    protected final UUID uuid;
    protected String name;
    protected String displayName;

    public PlayerInfo(UUID uuid, String name, String displayName) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }
}
