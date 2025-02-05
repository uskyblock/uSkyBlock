package us.talabrek.ultimateskyblock.api.model;

public class IslandLocation extends Model {
    protected final LocationType locationType;
    protected String world;
    protected double x;
    protected double y;
    protected double z;
    protected double pitch;
    protected double yaw;

    public IslandLocation(LocationType locationType, String world, double x, double y, double z, double pitch, double yaw) {
        this.locationType = locationType;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public IslandLocation(LocationType locationType) {
        this.locationType = locationType;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
        setDirty(true);
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        setDirty(true);
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        setDirty(true);
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
        setDirty(true);
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
        setDirty(true);
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
        setDirty(true);
    }

    public enum LocationType {
        WARP
    }
}
