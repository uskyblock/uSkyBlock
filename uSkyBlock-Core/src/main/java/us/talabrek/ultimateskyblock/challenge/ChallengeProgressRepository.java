package us.talabrek.ultimateskyblock.challenge;

import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.island.IslandKey;

import java.util.Map;

public interface ChallengeProgressRepository extends AutoCloseable {
    @NotNull Map<ChallengeKey, ChallengeCompletion> load(@NotNull IslandKey islandKey);

    void replace(@NotNull IslandKey islandKey, @NotNull Map<ChallengeKey, ChallengeCompletion> progress);

    boolean hasProgress(@NotNull IslandKey islandKey);

    void shutdown();

    @Override
    default void close() {
        shutdown();
    }
}
