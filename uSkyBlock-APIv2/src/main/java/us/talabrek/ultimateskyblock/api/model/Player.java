package us.talabrek.ultimateskyblock.api.model;

import java.util.UUID;

public class Player extends Model {
    protected final UUID uuid;
    protected String name;
    protected String displayName;
    protected boolean clearInventory = false;
    protected PendingPlayerOperations pendingOperations;

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

    public PendingPlayerOperations getPendingOperations() {
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
