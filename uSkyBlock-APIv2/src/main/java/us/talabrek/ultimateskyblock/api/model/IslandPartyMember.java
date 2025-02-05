package us.talabrek.ultimateskyblock.api.model;

import java.util.UUID;

public class IslandPartyMember extends Model {
    protected final UUID uuid;
    protected Role role;
    protected boolean canChangeBiome;
    protected boolean canToggleLock;
    protected boolean canChangeWarp;
    protected boolean canToggleWarp;
    protected boolean canInviteOthers;
    protected boolean canKickOthers;
    protected boolean canBanOthers;
    protected int maxAnimals;
    protected int maxMonsters;
    protected int maxVillagers;
    protected int maxGolems;

    public IslandPartyMember(UUID uuid, Role role, boolean canChangeBiome, boolean canToggleLock, boolean canChangeWarp, boolean canToggleWarp, boolean canInviteOthers, boolean canKickOthers, boolean canBanOthers, int maxAnimals, int maxMonsters, int maxVillagers, int maxGolems) {
        this.uuid = uuid;
        this.role = role;
        this.canChangeBiome = canChangeBiome;
        this.canToggleLock = canToggleLock;
        this.canChangeWarp = canChangeWarp;
        this.canToggleWarp = canToggleWarp;
        this.canInviteOthers = canInviteOthers;
        this.canKickOthers = canKickOthers;
        this.canBanOthers = canBanOthers;
        this.maxAnimals = maxAnimals;
        this.maxMonsters = maxMonsters;
        this.maxVillagers = maxVillagers;
        this.maxGolems = maxGolems;
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

    public boolean isCanChangeBiome() {
        return canChangeBiome;
    }

    public void setCanChangeBiome(boolean canChangeBiome) {
        this.canChangeBiome = canChangeBiome;
        setDirty(true);
    }

    public boolean isCanToggleLock() {
        return canToggleLock;
    }

    public void setCanToggleLock(boolean canToggleLock) {
        this.canToggleLock = canToggleLock;
        setDirty(true);
    }

    public boolean isCanChangeWarp() {
        return canChangeWarp;
    }

    public void setCanChangeWarp(boolean canChangeWarp) {
        this.canChangeWarp = canChangeWarp;
        setDirty(true);
    }

    public boolean isCanToggleWarp() {
        return canToggleWarp;
    }

    public void setCanToggleWarp(boolean canToggleWarp) {
        this.canToggleWarp = canToggleWarp;
        setDirty(true);
    }

    public boolean isCanInviteOthers() {
        return canInviteOthers;
    }

    public void setCanInviteOthers(boolean canInviteOthers) {
        this.canInviteOthers = canInviteOthers;
        setDirty(true);
    }

    public boolean isCanKickOthers() {
        return canKickOthers;
    }

    public void setCanKickOthers(boolean canKickOthers) {
        this.canKickOthers = canKickOthers;
        setDirty(true);
    }

    public boolean isCanBanOthers() {
        return canBanOthers;
    }

    public void setCanBanOthers(boolean canBanOthers) {
        this.canBanOthers = canBanOthers;
        setDirty(true);
    }

    public int getMaxAnimals() {
        return maxAnimals;
    }

    public void setMaxAnimals(int maxAnimals) {
        this.maxAnimals = maxAnimals;
        setDirty(true);
    }

    public int getMaxMonsters() {
        return maxMonsters;
    }

    public void setMaxMonsters(int maxMonsters) {
        this.maxMonsters = maxMonsters;
        setDirty(true);
    }

    public int getMaxVillagers() {
        return maxVillagers;
    }

    public void setMaxVillagers(int maxVillagers) {
        this.maxVillagers = maxVillagers;
        setDirty(true);
    }

    public int getMaxGolems() {
        return maxGolems;
    }

    public void setMaxGolems(int maxGolems) {
        this.maxGolems = maxGolems;
        setDirty(true);
    }

    public enum Role {
        MEMBER,
        LEADER
    }
}
