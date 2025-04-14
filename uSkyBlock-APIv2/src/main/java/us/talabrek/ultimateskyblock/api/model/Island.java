package us.talabrek.ultimateskyblock.api.model;

import org.bukkit.block.Biome;

import java.util.Map;
import java.util.UUID;

public class Island extends Model {
    protected final UUID uuid;
    protected final String name;
    protected UUID owner;
    protected boolean ignore = false;
    protected boolean locked = false;
    protected boolean warpActive = false;
    protected String regionVersion;
    protected String schematicName;

    protected double level = 0D;
    protected double scoreMultiplier = 1D;
    protected double scoreOffset = 0D;

    protected Biome biome;
    protected int leafBreaks = 0;
    protected int hopperCount = 0;

    protected IslandAccessList islandAccessList;
    protected IslandLimits islandLimits;
    protected IslandLocations islandLocations;
    protected IslandLog islandLog;
    protected IslandParty islandParty;

    /**
     * Creates a very basic Island object. This method should only be called when creating a NEW island
     * on a location where no island exists.
     * @param name Island name (legacy center coordinates)
     * @param owner UUID of the owner.
     */
    public Island(String name, UUID owner) {
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.owner = owner;

        this.islandAccessList = new IslandAccessList(this);
        this.islandLimits = new IslandLimits(this);
        this.islandLocations = new IslandLocations(this);
        this.islandLog = new IslandLog(this);
        this.islandParty = new IslandParty(this);

        this.islandLimits.setBlockLimits(Map.of());
        this.islandLimits.setPluginLimits(Map.of());
    }

    public Island(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setDirty(true);
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
        setDirty(true);
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        setDirty(true);
    }

    public boolean isWarpActive() {
        return warpActive;
    }

    public void setWarpActive(boolean warpActive) {
        this.warpActive = warpActive;
        setDirty(true);
    }

    public String getRegionVersion() {
        return regionVersion;
    }

    public void setRegionVersion(String regionVersion) {
        this.regionVersion = regionVersion;
        setDirty(true);
    }

    public String getSchematicName() {
        return schematicName;
    }

    public void setSchematicName(String schematicName) {
        this.schematicName = schematicName;
        setDirty(true);
    }

    public double getLevel() {
        return level;
    }

    public void setLevel(double level) {
        this.level = level;
        setDirty(true);
    }

    public double getScoreMultiplier() {
        return scoreMultiplier;
    }

    public void setScoreMultiplier(double scoreMultiplier) {
        this.scoreMultiplier = scoreMultiplier;
        setDirty(true);
    }

    public double getScoreOffset() {
        return scoreOffset;
    }

    public void setScoreOffset(double scoreOffset) {
        this.scoreOffset = scoreOffset;
        setDirty(true);
    }

    public Biome getBiome() {
        return biome;
    }

    public void setBiome(Biome biome) {
        this.biome = biome;
        setDirty(true);
    }

    public int getLeafBreaks() {
        return leafBreaks;
    }

    public void setLeafBreaks(int leafBreaks) {
        this.leafBreaks = leafBreaks;
        setDirty(true);
    }

    public int getHopperCount() {
        return hopperCount;
    }

    public void setHopperCount(int hopperCount) {
        this.hopperCount = hopperCount;
        setDirty(true);
    }

    public IslandAccessList getIslandAccessList() {
        if (islandAccessList == null) {
            islandAccessList = new IslandAccessList(this);
        }

        return islandAccessList;
    }

    public void setIslandAccessList(IslandAccessList islandAccessList) {
        if (this.islandAccessList == null) {
            this.islandAccessList = islandAccessList;
            return;
        }

        throw new IllegalStateException("IslandAccessList can only be set once.");
    }

    public IslandLimits getIslandLimits() {
        if (islandLimits == null) {
            islandLimits = new IslandLimits(this);
        }

        return islandLimits;
    }

    public void setIslandLimits(IslandLimits islandLimits) {
        if (this.islandLimits == null) {
            this.islandLimits = islandLimits;
            return;
        }

        throw new IllegalStateException("IslandLimits can only be set once.");
    }

    public IslandLocations getIslandLocations() {
        if (this.islandLocations == null) {
            islandLocations = new IslandLocations(this);
        }

        return islandLocations;
    }

    public void setIslandLocations(IslandLocations islandLocations) {
        if (this.islandLocations == null) {
            this.islandLocations = islandLocations;
            return;
        }

        throw new IllegalStateException("IslandLocations can only be set once.");
    }

    public IslandLog getIslandLog() {
        if (islandLog == null) {
            islandLog = new IslandLog(this);
        }
        return islandLog;
    }

    public void setIslandLog(IslandLog islandLog) {
        if (this.islandLog == null) {
            this.islandLog = islandLog;
            return;
        }

        throw new IllegalStateException("IslandLog can only be set once.");
    }

    public IslandParty getIslandParty() {
        if (islandParty == null) {
            islandParty = new IslandParty(this);
        }
        return islandParty;
    }

    public void setIslandParty(IslandParty islandParty) {
        if (this.islandParty == null) {
            this.islandParty = islandParty;
            return;
        }

        throw new IllegalStateException("IslandParty can only be set once.");
    }
}
