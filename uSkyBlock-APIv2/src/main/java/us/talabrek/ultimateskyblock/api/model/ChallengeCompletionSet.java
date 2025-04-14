package us.talabrek.ultimateskyblock.api.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChallengeCompletionSet extends Model {
    protected final UUID uuid;
    protected final CompletionSharing sharingType;
    protected ConcurrentMap<String, ChallengeCompletion> completionMap = new ConcurrentHashMap<>();

    public ChallengeCompletionSet(UUID uuid, CompletionSharing sharingType) {
        this.uuid = uuid;
        this.sharingType = sharingType;
    }

    public UUID getUuid() {
        return uuid;
    }

    public CompletionSharing getSharingType() {
        return sharingType;
    }

    public Map<String, ChallengeCompletion> getCompletionMap() {
        return Map.copyOf(completionMap);
    }

    public ChallengeCompletion getCompletion(String challengeName) {
        if (!completionMap.containsKey(challengeName)) {
            ChallengeCompletion completion = new ChallengeCompletion(uuid, challengeName);
            completionMap.put(challengeName, completion);
            setDirty(true);
        }

        return completionMap.get(challengeName);
    }

    public void setCompletion(String challengeName, ChallengeCompletion completion) {
        completionMap.put(challengeName, completion);
        setDirty(true);
    }

    public void reset() {
        completionMap.clear();
        setDirty(true);
    }

    @Override
    public boolean isDirty() {
        for (ChallengeCompletion completion : completionMap.values()) {
            if (completion.isDirty()) return true;
        }

        return dirty;
    }

    public enum CompletionSharing {
        ISLAND,
        PLAYER
    }
}
