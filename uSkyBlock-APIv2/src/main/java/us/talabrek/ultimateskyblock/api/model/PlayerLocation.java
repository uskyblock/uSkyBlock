package us.talabrek.ultimateskyblock.api.model;

import org.jetbrains.annotations.NotNull;

public class PlayerLocation extends Location {
    protected final LocationType locationType;

    public PlayerLocation(@NotNull LocationType locationType, @NotNull org.bukkit.Location location) {
        super(location);
        this.locationType = locationType;
    }

    public PlayerLocation(LocationType locationType, String world, double x, double y, double z, double pitch, double yaw) {
        super(world, x, y, z, pitch, yaw);
        this.locationType = locationType;
    }

    public PlayerLocation(LocationType locationType) {
        this.locationType = locationType;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public enum LocationType {
        HOME
    }
}
