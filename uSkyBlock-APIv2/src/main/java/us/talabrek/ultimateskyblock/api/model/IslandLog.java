package us.talabrek.ultimateskyblock.api.model;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class IslandLog extends Model {
    protected final Island island;
    protected ConcurrentSkipListSet<IslandLogLine> log = new ConcurrentSkipListSet<>();

    public IslandLog(Island island) {
        this.island = island;
    }

    public Island getIsland() {
        return island;
    }

    public Set<IslandLogLine> getLog() {
        return Set.copyOf(log);
    }

    public void log(IslandLogLine islandLogLine) {
        log.add(islandLogLine);
        setDirty(true);
    }
}
