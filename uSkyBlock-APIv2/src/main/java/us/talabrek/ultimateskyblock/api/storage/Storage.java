package us.talabrek.ultimateskyblock.api.storage;

import us.talabrek.ultimateskyblock.api.model.ChallengeCompletion;
import us.talabrek.ultimateskyblock.api.model.Island;
import us.talabrek.ultimateskyblock.api.model.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage {
    CompletableFuture<Void> saveChallengeCompletion(ChallengeCompletion challengeCompletion);
    CompletableFuture<UUID> getIslandByName(String name);
    CompletableFuture<Void> saveIsland(Island island);
    CompletableFuture<Player> getPlayer(UUID uuid);
    CompletableFuture<Player> getPlayer(String username);
    CompletableFuture<Void> savePlayer(Player player);
}
