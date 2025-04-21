package us.talabrek.ultimateskyblock.api.storage;

import us.talabrek.ultimateskyblock.api.model.ChallengeCompletionSet;
import us.talabrek.ultimateskyblock.api.model.Island;
import us.talabrek.ultimateskyblock.api.model.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage {
    CompletableFuture<ChallengeCompletionSet> getChallengeCompletion(UUID uuid);
    CompletableFuture<Void> saveChallengeCompletion(ChallengeCompletionSet challengeCompletion);
    CompletableFuture<Integer> deleteChallengeCompletion(UUID uuid);
    CompletableFuture<Integer> getIslandCount();
    CompletableFuture<Island> getIsland(UUID uuid);

    /**
     * Gets all island UUIDs in the database.
     * @return A set of island UUIDs.
     */
    CompletableFuture<Set<UUID>> getIslands();
    CompletableFuture<UUID> getIslandByName(String name);
    CompletableFuture<Void> saveIsland(Island island);
    CompletableFuture<Void> deleteIsland(Island island);
    CompletableFuture<Player> getPlayer(UUID uuid);
    CompletableFuture<Player> getPlayer(String username);
    CompletableFuture<List<String>> getPlayerBannedOn(UUID playerUuid);
    CompletableFuture<List<String>> getPlayerTrustedOn(UUID playerUuid);
    CompletableFuture<UUID> getPlayerIsland(UUID playerUuid);
    CompletableFuture<Void> savePlayer(Player player);
    CompletableFuture<Void> clearPlayer(UUID uuid);
}
