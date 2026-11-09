package us.talabrek.ultimateskyblock.update;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

/**
 * Stages a uSkyBlock version in the server's update folder, under the running plugin's jar name, so the server swaps
 * it in at its next start.
 */
@Singleton
public class UpdateStager {

    private final uSkyBlock plugin;
    private final ModrinthClient modrinth;
    private final Clock clock;
    private final Logger logger;
    private final AtomicBoolean staging = new AtomicBoolean();
    private volatile @Nullable StagedUpdate staged;

    /**
     * @param automatic Whether auto-stage staged it, rather than an admin.
     */
    public record StagedUpdate(@NotNull String version, boolean automatic) {
    }

    public enum Failure {
        NO_UPDATE_FOLDER("the server has no update folder apart from the plugins folder (settings.update-folder in bukkit.yml)"),
        NOT_WRITABLE("the update folder can't be written"),
        NO_FILE("Modrinth lists no file for it"),
        HASH_MISMATCH("the download doesn't match the SHA-512 hash Modrinth lists"),
        UNREACHABLE("the download from Modrinth failed"),
        BUSY("another update is being staged");

        private final String description;

        Failure(@NotNull String description) {
            this.description = description;
        }

        public @NotNull String description() {
            return description;
        }
    }

    /**
     * @param replaced The version the staging replaced, NULL if none was staged.
     * @param failure  Why nothing was staged, NULL if it was.
     */
    public record Result(
        @NotNull ModrinthVersion version,
        @Nullable Path file,
        @Nullable String replaced,
        @Nullable Failure failure
    ) {
        static @NotNull Result failed(@NotNull ModrinthVersion version, @NotNull Failure failure) {
            return new Result(version, null, null, failure);
        }
    }

    @Inject
    public UpdateStager(
        @NotNull uSkyBlock plugin,
        @NotNull ModrinthClient modrinth,
        @NotNull Clock clock,
        @NotNull @PluginLog Logger logger
    ) {
        this.plugin = plugin;
        this.modrinth = modrinth;
        this.clock = clock;
        this.logger = logger;
    }

    /**
     * Reads which version an earlier run staged, so it is still known after a {@code /usb reload}.
     */
    public void startup() {
        Path target = target();
        if (target == null || !Files.isRegularFile(target)) {
            return;
        }
        try (JarFile jar = new JarFile(target.toFile())) {
            ZipEntry pluginYml = jar.getEntry("plugin.yml");
            if (pluginYml != null) {
                try (InputStream in = jar.getInputStream(pluginYml)) {
                    staged = new StagedUpdate(new PluginDescriptionFile(in).getVersion(), false);
                }
            }
        } catch (IOException | InvalidDescriptionException ex) {
            logger.log(Level.FINE, "Can't read the staged update " + target, ex);
        }
    }

    /**
     * @return The version staged for the next restart, NULL if none is.
     */
    public @Nullable StagedUpdate staged() {
        return staged;
    }

    /**
     * Stages a version off the server thread.
     *
     * @param stagedBy Who staged it, for the log.
     */
    public @NotNull CompletableFuture<Result> stage(@NotNull ModrinthVersion version, @NotNull String stagedBy,
                                                    boolean automatic) {
        Path target = target();
        if (target == null) {
            return CompletableFuture.completedFuture(Result.failed(version, Failure.NO_UPDATE_FOLDER));
        }
        if (!staging.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(Result.failed(version, Failure.BUSY));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                Result result = stageInto(target, version);
                if (result.failure() == null) {
                    staged = new StagedUpdate(version.version(), automatic);
                    logger.info("Staged uSkyBlock " + version.version() + " as " + target
                        + " for the next restart (by " + stagedBy + ").");
                }
                return result;
            } finally {
                staging.set(false);
            }
        });
    }

    /**
     * Stages the regular update once it has been out for the delay, unless it or a newer version is staged already.
     * A new major version and a version that doesn't run on this server are never staged.
     *
     * @return The staging, or NULL if there is nothing to stage.
     */
    public @NotNull CompletableFuture<@Nullable Result> autoStage(@NotNull Updates updates, @NotNull Duration delay) {
        ModrinthVersion update = updates.update();
        StagedUpdate current = staged;
        if (update == null || update.published().plus(delay).isAfter(clock.instant())
            || (current != null && !Updates.isNewer(current.version(), update.version()))) {
            return CompletableFuture.completedFuture(null);
        }
        return stage(update, "auto-stage", true).thenApply(result -> {
            if (result.failure() != null) {
                logger.warning("Could not stage uSkyBlock " + update.version() + " automatically: "
                    + result.failure().description() + ".");
            }
            return result;
        });
    }

    /**
     * @return Where the staged jar goes, NULL if the server has no update folder apart from the plugins folder.
     */
    public @Nullable Path target() {
        File updateFolder = plugin.getServer().getUpdateFolderFile();
        Path jar = plugin.getPluginJar();
        if (updateFolder == null || updateFolder.getName().isBlank()) {
            return null;
        }
        Path folder = updateFolder.toPath().toAbsolutePath().normalize();
        Path jarFolder = jar.toAbsolutePath().normalize().getParent();
        // Paper may load a remapped copy of the jar from outside the plugins folder; the data folder stays in it.
        Path pluginsFolder = plugin.getDataFolder().toPath().toAbsolutePath().normalize().getParent();
        if (folder.equals(jarFolder) || folder.equals(pluginsFolder)) {
            return null;
        }
        return folder.resolve(jar.getFileName().toString());
    }

    private @NotNull Result stageInto(@NotNull Path target, @NotNull ModrinthVersion version) {
        ModrinthVersion.DownloadFile file = version.file();
        if (file == null) {
            return Result.failed(version, Failure.NO_FILE);
        }
        Path temp;
        try {
            Files.createDirectories(target.getParent());
            temp = Files.createTempFile(target.getParent(), "uSkyBlock-", ".jar.part");
        } catch (IOException ex) {
            return Result.failed(version, Failure.NOT_WRITABLE);
        }
        try {
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            try (OutputStream out = new DigestOutputStream(Files.newOutputStream(temp), sha512)) {
                modrinth.download(file.url(), out);
            } catch (IOException ex) {
                return Result.failed(version, Failure.UNREACHABLE);
            }
            if (!HexFormat.of().formatHex(sha512.digest()).equalsIgnoreCase(file.sha512())) {
                return Result.failed(version, Failure.HASH_MISMATCH);
            }
            StagedUpdate previous = staged;
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                return Result.failed(version, Failure.NOT_WRITABLE);
            }
            return new Result(version, target, previous != null ? previous.version() : null, null);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("The JVM has no SHA-512", ex);
        } finally {
            deleteQuietly(temp);
        }
    }

    private void deleteQuietly(@NotNull Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            logger.log(Level.FINE, "Can't delete " + file, ex);
        }
    }
}
