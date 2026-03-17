package us.talabrek.ultimateskyblock.challenge;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.island.IslandKey;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.BackupFileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

final class ChallengeProgressMigration {
    private static final String LEGACY_IMPORT_COMPLETED_KEY = "legacy_yaml_import_completed";
    private record LegacyPlayerProgress(Path path, YamlConfiguration config) {}

    private final uSkyBlock plugin;
    private final ChallengeLogic challengeLogic;
    private final ChallengeProgressRepository repository;
    private final Path legacyStorageDir;
    private final Path playerStorageDir;
    private final Path islandStorageDir;

    ChallengeProgressMigration(
        @NotNull uSkyBlock plugin,
        @NotNull ChallengeLogic challengeLogic,
        @NotNull ChallengeProgressRepository repository
    ) {
        this.plugin = plugin;
        this.challengeLogic = challengeLogic;
        this.repository = repository;
        this.legacyStorageDir = plugin.getDataFolder().toPath().resolve("completion");
        this.playerStorageDir = plugin.getDataFolder().toPath().resolve("players");
        this.islandStorageDir = plugin.getDataFolder().toPath().resolve("islands");
    }

    void migrateLegacyDataEagerly() {
        if (repository.getMetadata(LEGACY_IMPORT_COMPLETED_KEY).isPresent()) {
            return;
        }

        Map<IslandKey, Map<ChallengeKey, ChallengeCompletion>> migratedProgress = new HashMap<>();
        Map<IslandKey, List<Path>> migratedFiles = new HashMap<>();
        Map<IslandKey, List<LegacyPlayerProgress>> migratedPlayerConfigs = new HashMap<>();

        migrateLegacyCompletionFiles(migratedProgress, migratedFiles);
        migrateLegacyPlayerFiles(migratedProgress, migratedPlayerConfigs);

        int migratedIslands = 0;
        for (Map.Entry<IslandKey, Map<ChallengeKey, ChallengeCompletion>> entry : migratedProgress.entrySet()) {
            IslandKey islandKey = entry.getKey();
            Map<ChallengeKey, ChallengeCompletion> merged = loadOrPopulateProgress(islandKey);
            mergeProgress(merged, entry.getValue());
            repository.replace(islandKey, merged);

            for (Path file : migratedFiles.getOrDefault(islandKey, List.of())) {
                backupLegacyFile(file);
            }
            for (LegacyPlayerProgress playerProgress : migratedPlayerConfigs.getOrDefault(islandKey, List.of())) {
                playerProgress.config().set("player.challenges", null);
                try {
                    playerProgress.config().save(playerProgress.path().toFile());
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Unable to clear migrated legacy player challenge progress from " + playerProgress.path(), e);
                }
            }
            migratedIslands++;
        }
        cleanupLegacyCompletionDir();
        repository.putMetadata(LEGACY_IMPORT_COMPLETED_KEY, "true");
        plugin.getLogger().info("Migrated legacy challenge progress for " + migratedIslands + " island(s) into SQLite storage.");
    }

    private @NotNull Map<ChallengeKey, ChallengeCompletion> loadOrPopulateProgress(IslandKey islandKey) {
        Map<ChallengeKey, ChallengeCompletion> challengeMap = new HashMap<>();
        challengeLogic.populateChallenges(challengeMap);
        challengeMap.putAll(repository.load(islandKey));
        return challengeMap;
    }

    private void migrateLegacyCompletionFiles(
        @NotNull Map<IslandKey, Map<ChallengeKey, ChallengeCompletion>> migratedProgress,
        @NotNull Map<IslandKey, List<Path>> migratedFiles
    ) {
        if (!Files.isDirectory(legacyStorageDir)) {
            return;
        }
        Map<UUID, IslandKey> islandsByLeader = loadIslandsByLeaderUuid();
        try (var files = Files.list(legacyStorageDir)) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".yml"))
                .forEach(path -> {
                    IslandKey islandKey = resolveLegacyCompletionOwner(path, islandsByLeader);
                    if (islandKey == null) {
                        plugin.getLogger().warning("Unable to resolve legacy challenge progress owner for " + path.getFileName() + ". Leaving file in place.");
                        return;
                    }
                    mergeProgress(
                        migratedProgress.computeIfAbsent(islandKey, ignored -> new HashMap<>()),
                        loadLegacyFile(path.toFile())
                    );
                    migratedFiles.computeIfAbsent(islandKey, ignored -> new java.util.ArrayList<>()).add(path);
                });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Unable to scan legacy challenge progress directory " + legacyStorageDir, e);
        }
    }

    private void migrateLegacyPlayerFiles(
        @NotNull Map<IslandKey, Map<ChallengeKey, ChallengeCompletion>> migratedProgress,
        @NotNull Map<IslandKey, List<LegacyPlayerProgress>> migratedPlayerConfigs
    ) {
        if (!Files.isDirectory(playerStorageDir)) {
            return;
        }
        try (var files = Files.list(playerStorageDir)) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".yml"))
                .forEach(path -> {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(path.toFile());
                    ConfigurationSection challengeSection = config.getConfigurationSection("player.challenges");
                    if (challengeSection == null || challengeSection.getKeys(false).isEmpty()) {
                        return;
                    }
                    IslandKey islandKey = resolvePlayerIslandKey(config);
                    if (islandKey == null) {
                        plugin.getLogger().warning("Unable to resolve island owner for legacy player challenge progress in " + path.getFileName() + ". Leaving data in place.");
                        return;
                    }
                    mergeProgress(
                        migratedProgress.computeIfAbsent(islandKey, ignored -> new HashMap<>()),
                        loadFromConfiguration(challengeSection)
                    );
                    migratedPlayerConfigs.computeIfAbsent(islandKey, ignored -> new java.util.ArrayList<>())
                        .add(new LegacyPlayerProgress(path, config));
                });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Unable to scan player data directory " + playerStorageDir + " for legacy challenge progress", e);
        }
    }

    private @NotNull Map<UUID, IslandKey> loadIslandsByLeaderUuid() {
        Map<UUID, IslandKey> islandsByLeader = new HashMap<>();
        if (!Files.isDirectory(islandStorageDir)) {
            return islandsByLeader;
        }
        try (var files = Files.list(islandStorageDir)) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".yml"))
                .forEach(path -> {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(path.toFile());
                    String leaderUuid = config.getString("party.leader-uuid", null);
                    if (leaderUuid == null || leaderUuid.isBlank()) {
                        return;
                    }
                    try {
                        String fileName = path.getFileName().toString();
                        String islandName = fileName.substring(0, fileName.length() - 4);
                        islandsByLeader.put(UUID.fromString(leaderUuid), IslandKey.fromIslandName(islandName));
                    } catch (IllegalArgumentException ignored) {
                        // Ignore invalid leader UUIDs or non-island-named files.
                    }
                });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Unable to scan island data directory " + islandStorageDir, e);
        }
        return islandsByLeader;
    }

    private IslandKey resolveLegacyCompletionOwner(@NotNull Path legacyFile, @NotNull Map<UUID, IslandKey> islandsByLeader) {
        String fileName = legacyFile.getFileName().toString();
        String basename = fileName.substring(0, fileName.length() - 4);
        try {
            return IslandKey.fromIslandName(basename);
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        try {
            return islandsByLeader.get(UUID.fromString(basename));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private IslandKey resolvePlayerIslandKey(@NotNull YamlConfiguration config) {
        int islandY = config.getInt("player.islandY", 0);
        if (islandY == 0) {
            return null;
        }
        int islandX = config.getInt("player.islandX", 0);
        int islandZ = config.getInt("player.islandZ", 0);
        try {
            return IslandKey.fromIslandName(islandX + "," + islandZ);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private @NotNull Map<ChallengeKey, ChallengeCompletion> loadLegacyFile(@NotNull File configFile) {
        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(configFile);
        if (fileConfiguration.getRoot() == null) {
            return Collections.emptyMap();
        }
        return loadFromConfiguration(fileConfiguration.getRoot());
    }

    private @NotNull Map<ChallengeKey, ChallengeCompletion> loadFromConfiguration(ConfigurationSection root) {
        Map<ChallengeKey, ChallengeCompletion> challengeMap = new HashMap<>();
        if (root != null) {
            for (String challengeName : root.getKeys(false)) {
                long firstCompleted = root.getLong(challengeName + ".firstCompleted", 0);
                Instant firstCompletedDuration = firstCompleted > 0 ? Instant.ofEpochMilli(firstCompleted) : null;
                ChallengeKey challengeId = ChallengeKey.of(challengeName);
                challengeMap.put(challengeId, new ChallengeCompletion(
                    challengeId,
                    firstCompletedDuration,
                    root.getInt(challengeName + ".timesCompleted", 0),
                    root.getInt(challengeName + ".timesCompletedSinceTimer", 0)
                ));
            }
        }
        return challengeMap;
    }

    private void mergeProgress(@NotNull Map<ChallengeKey, ChallengeCompletion> target, @NotNull Map<ChallengeKey, ChallengeCompletion> incoming) {
        for (Map.Entry<ChallengeKey, ChallengeCompletion> entry : incoming.entrySet()) {
            ChallengeCompletion existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), entry.getValue());
                continue;
            }
            target.put(entry.getKey(), mergeCompletion(entry.getKey(), existing, entry.getValue()));
        }
    }

    private ChallengeCompletion mergeCompletion(
        @NotNull ChallengeKey challengeKey,
        @NotNull ChallengeCompletion left,
        @NotNull ChallengeCompletion right
    ) {
        Instant cooldownUntil = maxInstant(left.cooldownUntil(), right.cooldownUntil());
        int timesCompleted = Math.max(left.getTimesCompleted(), right.getTimesCompleted());
        int timesCompletedInCooldown = Math.max(left.getTimesCompletedInCooldown(), right.getTimesCompletedInCooldown());
        return new ChallengeCompletion(challengeKey, cooldownUntil, timesCompleted, timesCompletedInCooldown);
    }

    private Instant maxInstant(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private void backupLegacyFile(@NotNull Path source) {
        try {
            BackupFileUtil.moveToBackup(
                plugin.getDataFolder().toPath(),
                source,
                "completion/" + source.getFileName()
            );
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Unable to back up legacy challenge progress file " + source, e);
        }
    }

    private void cleanupLegacyCompletionDir() {
        if (!Files.isDirectory(legacyStorageDir)) {
            return;
        }
        try (var files = Files.list(legacyStorageDir)) {
            if (files.findAny().isEmpty()) {
                Files.delete(legacyStorageDir);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Unable to clean up legacy completion directory " + legacyStorageDir, e);
        }
    }
}
