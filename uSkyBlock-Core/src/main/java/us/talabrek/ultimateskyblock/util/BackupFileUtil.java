package us.talabrek.ultimateskyblock.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class BackupFileUtil {
    private static final String BACKUP_DIR_NAME = "backup";

    private BackupFileUtil() {
    }

    public static @NotNull Path copyToBackup(@NotNull Path pluginDataDir, @NotNull Path source,
                                             @NotNull String backupFileName) throws IOException {
        Path target = resolveBackupPath(pluginDataDir, backupFileName);
        Files.copy(source, target);
        return target;
    }

    public static @NotNull Path moveToBackup(@NotNull Path pluginDataDir, @NotNull Path source,
                                             @NotNull String backupFileName) throws IOException {
        Path target = resolveBackupPath(pluginDataDir, backupFileName);
        moveIntoPlace(source, target);
        return target;
    }

    public static @NotNull Path resolveBackupPath(@NotNull Path pluginDataDir, @NotNull String backupFileName) throws IOException {
        Path backupDir = pluginDataDir.resolve(BACKUP_DIR_NAME);
        Files.createDirectories(backupDir);

        Path candidate = backupDir.resolve(backupFileName);
        if (!Files.exists(candidate)) {
            return candidate;
        }

        String fileName = candidate.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
        String extension = extensionIndex >= 0 ? fileName.substring(extensionIndex) : "";

        for (int suffix = 1; ; suffix++) {
            candidate = backupDir.resolve(baseName + "-" + suffix + extension);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
    }

    private static void moveIntoPlace(@NotNull Path source, @NotNull Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target);
        }
    }
}
