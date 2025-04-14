package us.talabrek.ultimateskyblock.api.model;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class Location extends Model {
    protected String world;
    protected double x;
    protected double y;
    protected double z;
    protected double pitch;
    protected double yaw;

    protected Location() {}

    protected Location(@NotNull org.bukkit.Location location) {
        Objects.requireNonNull(location);
        Objects.requireNonNull(location.getWorld());

        this.world = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.pitch = location.getPitch();
        this.yaw = location.getYaw();
    }

    protected Location(String world, double x, double y, double z, double pitch, double yaw) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
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

    public org.bukkit.Location asBukkitLocation() {
        return new org.bukkit.Location(
            Bukkit.getWorld(this.getWorld()),
            this.getX(),
            this.getY(),
            this.getZ(),
            (float) this.getYaw(),
            (float) this.getPitch()
        );
    }
}
