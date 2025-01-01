package us.talabrek.ultimateskyblock.storage;

/**
 * A SQL-related runnable _is_ allowed to throw an exception, which will be handled correctly.
 */
@FunctionalInterface
public interface StorageRunnable {
    void run() throws Exception;
}
