package us.talabrek.ultimateskyblock.api.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class IslandLocations extends Model {
    protected final Island island;
    protected ConcurrentMap<IslandLocation.LocationType, IslandLocation> locations = new ConcurrentHashMap<>();

    public IslandLocations(Island island) {
        this.island = island;
    }

    public Island getIsland() {
        return island;
    }

    public ConcurrentMap<IslandLocation.LocationType, IslandLocation> getLocations() {
        return locations;
    }

    public IslandLocation getLocation(IslandLocation.LocationType type) {
        return locations.get(type);
    }

    public void addLocation(IslandLocation.LocationType type, IslandLocation location) {
        locations.put(type, location);
        setDirty(true);
    }

    public boolean removeLocation(IslandLocation.LocationType type) {
        if (locations.remove(type) != null) {
            setDirty(true);
            return true;
        }

        return false;
    }
}
