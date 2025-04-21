package us.talabrek.ultimateskyblock.api.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlayerLocations extends Model {
    protected final Player player;
    protected ConcurrentMap<PlayerLocation.LocationType, PlayerLocation> locations = new ConcurrentHashMap<>();

    public PlayerLocations(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public ConcurrentMap<PlayerLocation.LocationType, PlayerLocation> getLocations() {
        return locations;
    }

    public PlayerLocation getLocation(PlayerLocation.LocationType type) {
        return locations.get(type);
    }

    public void addLocation(PlayerLocation.LocationType type, PlayerLocation location) {
        locations.put(type, location);
        setDirty(true);
    }

    public boolean removeLocation(PlayerLocation.LocationType type) {
        if (locations.remove(type) != null) {
            setDirty(true);
            return true;
        }

        return false;
    }

    public void clearLocations() {
        locations.clear();
        setDirty(true);
    }
}
