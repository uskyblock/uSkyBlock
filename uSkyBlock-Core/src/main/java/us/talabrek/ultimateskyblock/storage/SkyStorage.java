package us.talabrek.ultimateskyblock.storage;

import us.talabrek.ultimateskyblock.api.model.ChallengeCompletion;
import us.talabrek.ultimateskyblock.api.model.Island;
import us.talabrek.ultimateskyblock.api.model.Player;
import us.talabrek.ultimateskyblock.api.storage.Storage;
import us.talabrek.ultimateskyblock.api.storage.StorageRunnable;
import us.talabrek.ultimateskyblock.storage.sql.H2Connection;
import us.talabrek.ultimateskyblock.storage.sql.SqlStorage;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ForkJoinPool;

public class SkyStorage implements Storage {
    protected final uSkyBlock plugin;
    protected SqlStorage storage;

    protected final ForkJoinPool pool;

    public SkyStorage(uSkyBlock plugin) {
        this.plugin = plugin;
        this.pool = new ForkJoinPool(8);

        try {
            storage = new H2Connection(plugin, plugin.getDataFolder().toPath().resolve("uskyblock"));
            storage.initialize();
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

    @Override
    public CompletableFuture<Void> saveChallengeCompletion(ChallengeCompletion challengeCompletion) {
        return future(() -> storage.saveChallengeCompletion(challengeCompletion));
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
    public CompletableFuture<Player> getPlayer(UUID uuid) {
        return future(() -> storage.getPlayer(uuid));
    }

    @Override
    public CompletableFuture<Player> getPlayer(String username) {
        return future(() -> storage.getPlayer(username));
    }

    @Override
    public CompletableFuture<Void> savePlayer(Player player) {
        return future(() -> storage.savePlayer(player));
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
