package us.talabrek.ultimateskyblock.storage;

import us.talabrek.ultimateskyblock.api.model.PlayerInfo;
import us.talabrek.ultimateskyblock.storage.sql.H2Connection;
import us.talabrek.ultimateskyblock.storage.sql.SqlStorage;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ForkJoinPool;

public class Storage {
    protected final uSkyBlock plugin;
    protected SqlStorage storage;

    protected final ForkJoinPool pool;

    public Storage(uSkyBlock plugin) {
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

    public CompletableFuture<PlayerInfo> getPlayerInfo(UUID uuid) {
        return future(() -> storage.getPlayerInfo(uuid));
    }

    public CompletableFuture<PlayerInfo> getPlayerInfo(String username) {
        return future(() -> storage.getPlayerInfo(username));
    }

    public CompletableFuture<Void> savePlayerInfo(PlayerInfo playerInfo) {
        return future(() -> storage.savePlayerInfo(playerInfo));
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
