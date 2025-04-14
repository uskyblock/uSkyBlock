package us.talabrek.ultimateskyblock.api.model;

import java.util.UUID;

public class Player extends Model {
    protected final UUID uuid;
    protected String name;
    protected String displayName;
    protected boolean clearInventory = false;

    protected PlayerLocations playerLocations;
    protected PendingPlayerOperations pendingOperations;
    protected PlayerPermissions permissions;

    public Player(UUID uuid, String name, String displayName, boolean clearInventory, PendingPlayerOperations pendingOperations) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
        this.clearInventory = clearInventory;
        this.pendingOperations = pendingOperations;
    }

    public Player(UUID uuid, String name, String displayName) {
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

    public void setName(String name) {
        this.name = name;
        setDirty(true);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        setDirty(true);
    }

    public boolean isClearInventory() {
        return clearInventory;
    }

    public void setClearInventory(boolean clearInventory) {
        this.clearInventory = clearInventory;
        setDirty(true);
    }

    public PlayerLocations getPlayerLocations() {
        if (this.playerLocations == null) {
            playerLocations = new PlayerLocations(this);
        }

        return playerLocations;
    }

    public void setPlayerLocations(PlayerLocations playerLocations) {
        if (this.playerLocations == null) {
            this.playerLocations = playerLocations;
            return;
        }

        throw new IllegalStateException("PlayerLocations can only be set once.");
    }

    public PlayerPermissions getPlayerPermissions() {
        if (this.permissions == null) {
            permissions = new PlayerPermissions(this);
        }

        return permissions;
    }

    public void setPlayerPermissions(PlayerPermissions permissions) {
        if (this.permissions == null) {
            this.permissions = permissions;
            return;
        }

        throw new IllegalStateException("PlayerPermissions can only be set once.");
    }

    public PendingPlayerOperations getPlayerPendingOperations() {
        if (pendingOperations == null) {
            pendingOperations = new PendingPlayerOperations(this);
        }
        return pendingOperations;
    }

    public void setPendingOperations(PendingPlayerOperations pendingOperations) {
        if (this.pendingOperations == null) {
            this.pendingOperations = pendingOperations;
            return;
        }

        throw new IllegalStateException("PendingPlayerOperations can only be set once.");
    }
}
