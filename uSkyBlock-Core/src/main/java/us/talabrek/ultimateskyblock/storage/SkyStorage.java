package us.talabrek.ultimateskyblock.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import us.talabrek.ultimateskyblock.api.model.ChallengeCompletionSet;
import us.talabrek.ultimateskyblock.api.model.Island;
import us.talabrek.ultimateskyblock.api.model.Player;
import us.talabrek.ultimateskyblock.api.IslandLevel;
import us.talabrek.ultimateskyblock.api.storage.Storage;
import us.talabrek.ultimateskyblock.api.storage.StorageRunnable;
import us.talabrek.ultimateskyblock.imports.storage.CompletionImporter;
import us.talabrek.ultimateskyblock.imports.storage.IslandImporter;
import us.talabrek.ultimateskyblock.imports.storage.PlayerImporter;
import us.talabrek.ultimateskyblock.storage.sql.H2Connection;
import us.talabrek.ultimateskyblock.storage.sql.SqlStorage;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ForkJoinPool;

@Singleton
public class SkyStorage implements Storage {
    protected final uSkyBlock plugin;
    protected final Logger logger;

    protected SqlStorage storage;
    protected final ForkJoinPool pool;

    @Inject
    public SkyStorage(@NotNull uSkyBlock plugin, @NotNull Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.pool = new ForkJoinPool(8);

        try {
            storage = new H2Connection(plugin, plugin.getDataFolder().toPath().resolve("uskyblock"));
            storage.initialize();

            convertFileStorageToSQL();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to connect to database.", ex);
        }
    }

    /**
     * Destructor method to handle an orderly shutdown process of the databaase connections.
     */
    public void destruct() {
        try {
            pool.shutdown();
            storage.close();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to close database.", ex);
        }
    }

    private void convertFileStorageToSQL() {
        if (Files.exists(plugin.getDataFolder().toPath().resolve("uuid2name.yml")) || Files.exists(plugin.getDataFolder().toPath().resolve("players"))) {
            plugin.getLog4JLogger().info("Importing old uuid2name.yml...");
            new PlayerImporter(plugin, this);
        }

        if (Files.exists(plugin.getDataFolder().toPath().resolve("islands"))) {
            plugin.getLog4JLogger().info("Importing old islands...");
            new IslandImporter(plugin, this);
        }

        if (Files.exists(plugin.getDataFolder().toPath().resolve("completion"))) {
            plugin.getLog4JLogger().info("Importing old completions...");
            new CompletionImporter(plugin, this);
        }
    }

    @Override
    public CompletableFuture<ChallengeCompletionSet> getChallengeCompletion(UUID uuid) {
        return future(() -> storage.getChallengeCompletion(uuid));
    }

    @Override
    public CompletableFuture<Void> saveChallengeCompletion(ChallengeCompletionSet challengeCompletion) {
        return future(() -> storage.saveChallengeCompletion(challengeCompletion));
    }

    @Override
    public CompletableFuture<Integer> deleteChallengeCompletion(UUID uuid) {
        return future(() -> storage.deleteChallengeCompletion(uuid));
    }

    @Override
    public CompletableFuture<Integer> getIslandCount() {
        return future(() -> storage.getIslandCount());
    }

    @Override
    public CompletableFuture<Island> getIsland(UUID uuid) {
        return future(() -> storage.getIsland(uuid));
    }

    @Override
    public CompletableFuture<Set<UUID>> getIslands() {
        return future(() -> storage.getIslands());
    }

    @Override
    public CompletableFuture<UUID> getIslandByName(String name) {
        return future(() -> storage.getIslandByName(name));
    }

    @Override
    public CompletableFuture<Void> saveIsland(Island island) {
        return future(() -> storage.saveIsland(island));
    }

    @Override
    public CompletableFuture<Void> deleteIsland(Island island) {
        return future(() -> storage.deleteIsland(island));
    }

    public CompletableFuture<Set<IslandLevel>> getIslandTop(double levelCutOff) {
        return future(() -> storage.getIslandTop(levelCutOff));
    }

    @Override
    public CompletableFuture<Player> getPlayer(UUID uuid) {
        return future(() -> storage.getPlayer(uuid));
    }

    @Override
    public CompletableFuture<Player> getPlayer(String username) {
        return future(() -> storage.getPlayer(username));
    }

    @Override
    public CompletableFuture<List<String>> getPlayerBannedOn(UUID playerUuid) {
        return future(() -> storage.getPlayerBannedOn(playerUuid));
    }

    @Override
    public CompletableFuture<List<String>> getPlayerTrustedOn(UUID playerUuid) {
        return future(() -> storage.getPlayerTrustedOn(playerUuid));
    }

    @Override
    public CompletableFuture<UUID> getPlayerIsland(UUID playerUuid) {
        return future(() -> storage.getPlayerIsland(playerUuid));
    }

    @Override
    public CompletableFuture<Void> savePlayer(Player player) {
        return future(() -> storage.savePlayer(player));
    }

    @Override
    public CompletableFuture<Void> clearPlayer(UUID uuid) {
        return future(() -> storage.clearPlayer(uuid));
    }

    private <T> CompletableFuture<T> future(Callable<T> callable) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            } catch (Exception ex) {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                throw new CompletionException(ex);
            }
        }, pool);
    }

    private CompletableFuture<Void> future(StorageRunnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Exception ex) {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                throw new CompletionException(ex);
            }
        }, pool);
    }
}
