package us.talabrek.ultimateskyblock.api.model;

import java.util.HashSet;
import java.util.Set;

public class PendingPlayerOperations extends Model {
    protected final Player player;
    protected Set<PendingPlayerOperation> pendingOperations = new HashSet<>();

    public PendingPlayerOperations(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Set<PendingPlayerOperation> getPendingOperations() {
        return Set.copyOf(pendingOperations);
    }

    public void addPendingOperation(PendingPlayerOperation operation) {
        pendingOperations.add(operation);
        setDirty(true);
    }
}
