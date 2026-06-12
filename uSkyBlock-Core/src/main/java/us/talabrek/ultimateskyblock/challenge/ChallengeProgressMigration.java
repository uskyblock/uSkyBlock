package us.talabrek.ultimateskyblock.challenge;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

final class ChallengeProgressMigration {
    private static final String LEGACY_IMPORT_COMPLETED_KEY = "legacy_yaml_import_completed";

    private record LegacyPlayerProgress(Path path, YamlConfiguration config) {}

    private record IslandUuidIndex(Map<UUID, IslandKey> byLeader, Map<UUID, IslandKey> byMember) {}

    private record CompletionScanResult(Set<IslandKey> islands, Set<UUID> players) {}

    private static final class MigrationCounters {
        private final AtomicInteger unmapped = new AtomicInteger();
        private final AtomicInteger residue = new AtomicInteger();
    }

    private final uSkyBlock plugin;
    private final ChallengeLogic challengeLogic;
    private final ChallengeProgressRepository repository;
    private final boolean legacyPlayerSharing;
    private final Path legacyStorageDir;
    private final Path playerStorageDir;
    private final Path islandStorageDir;

    ChallengeProgressMigration(
        @NotNull uSkyBlock plugin,
        @NotNull ChallengeLogic challengeLogic,
        @NotNull ChallengeProgressRepository repository,
        boolean legacyPlayerSharing
    ) {
        this.plugin = plugin;
        this.challengeLogic = challengeLogic;
        this.repository = repository;
        this.legacyPlayerSharing = legacyPlayerSharing;
        this.legacyStorageDir = plugin.getDataFolder().toPath().resolve("completion");
        this.playerStorageDir = plugin.getDataFolder().toPath().resolve("players");
        this.islandStorageDir = plugin.getDataFolder().toPath().resolve("islands");
    }

    void migrateLegacyDataEagerly() {
        if (repository.getMetadata(LEGACY_IMPORT_COMPLETED_KEY).isPresent()) {
            return;
        }

        Map<IslandKey, Map<ChallengeId, ChallengeCompletion>> migratedProgress = new HashMap<>();
        Map<IslandKey, List<Path>> migratedFiles = new HashMap<>();
        Map<IslandKey, List<LegacyPlayerProgress>> migratedPlayerConfigs = new HashMap<>();
        MigrationCounters counters = new MigrationCounters();

        CompletionScanResult completionFiles = migrateLegacyCompletionFiles(migratedProgress, migratedFiles, counters);
        migrateLegacyPlayerFiles(migratedProgress, migratedPlayerConfigs, completionFiles, counters);

        int migratedIslands = 0;
        for (Map.Entry<IslandKey, Map<ChallengeId, ChallengeCompletion>> entry : migratedProgress.entrySet()) {
            IslandKey islandKey = entry.getKey();
            Map<ChallengeId, ChallengeCompletion> merged = loadOrPopulateProgress(islandKey);
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
        if (counters.unmapped.get() > 0) {
            plugin.getLogger().warning("Skipped " + counters.unmapped.get()
                + " legacy challenge progress source(s) that could not be mapped to an island."
                + " The files were left in place; see the release notes for details.");
        }
        if (counters.residue.get() > 0) {
            plugin.getLogger().info("Left " + counters.residue.get()
                + " stale legacy challenge progress source(s) in place; the previous version did not use them either.");
        }
    }

    private @NotNull Map<ChallengeId, ChallengeCompletion> loadOrPopulateProgress(IslandKey islandKey) {
        Map<ChallengeId, ChallengeCompletion> challengeMap = new HashMap<>();
        challengeLogic.populateChallenges(challengeMap);
        challengeMap.putAll(repository.load(islandKey));
        return challengeMap;
    }

    private @NotNull CompletionScanResult migrateLegacyCompletionFiles(
        @NotNull Map<IslandKey, Map<ChallengeId, ChallengeCompletion>> migratedProgress,
        @NotNull Map<IslandKey, List<Path>> migratedFiles,
        @NotNull MigrationCounters counters
    ) {
        CompletionScanResult result = new CompletionScanResult(new HashSet<>(), new HashSet<>());
        if (!Files.isDirectory(legacyStorageDir)) {
            return result;
        }
        IslandUuidIndex islandIndex = loadIslandUuidIndex();
        try (var files = Files.list(legacyStorageDir)) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".yml"))
                .forEach(path -> {
                    IslandKey islandKey = resolveLegacyCompletionOwner(path, islandIndex, result, counters);
                    if (islandKey == null) {
                        return;
                    }
                    mergeProgress(
                        migratedProgress.computeIfAbsent(islandKey, ignored -> new HashMap<>()),
                        loadLegacyFile(path.toFile())
                    );
                    migratedFiles.computeIfAbsent(islandKey, ignored -> new java.util.ArrayList<>()).add(path);
                    result.islands().add(islandKey);
                });
        } catch (Exception e) {
            throw new IllegalStateException("Unable to scan legacy challenge progress directory " + legacyStorageDir, e);
        }
        return result;
    }

    private void migrateLegacyPlayerFiles(
        @NotNull Map<IslandKey, Map<ChallengeId, ChallengeCompletion>> migratedProgress,
        @NotNull Map<IslandKey, List<LegacyPlayerProgress>> migratedPlayerConfigs,
        @NotNull CompletionScanResult completionFiles,
        @NotNull MigrationCounters counters
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
                    if (config.getInt("player.islandY", 0) == 0) {
                        // The player has no island to attach the progress to; the previous version
                        // never read this data either.
                        counters.residue.incrementAndGet();
                        return;
                    }
                    IslandKey islandKey = resolvePlayerIslandKey(config);
                    if (islandKey == null) {
                        plugin.getLogger().warning("Unable to resolve island owner for legacy player challenge progress in " + path.getFileName() + ". Leaving data in place.");
                        counters.unmapped.incrementAndGet();
                        return;
                    }
                    if (isPlayerYmlSuperseded(path, islandKey, completionFiles)) {
                        // The previous version only fell back to the player-yml data when no
                        // completion file existed (per player under player sharing, per island
                        // under island sharing), so importing it would resurrect stale state.
                        counters.residue.incrementAndGet();
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
            throw new IllegalStateException("Unable to scan player data directory " + playerStorageDir + " for legacy challenge progress", e);
        }
    }

    private @NotNull IslandUuidIndex loadIslandUuidIndex() {
        Map<UUID, IslandKey> byLeader = new HashMap<>();
        Map<UUID, IslandKey> byMember = new HashMap<>();
        if (!Files.isDirectory(islandStorageDir)) {
            return new IslandUuidIndex(byLeader, byMember);
        }
        try (var files = Files.list(islandStorageDir)) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".yml"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    IslandKey islandKey;
                    try {
                        islandKey = IslandKey.fromIslandName(fileName.substring(0, fileName.length() - 4));
                    } catch (IllegalArgumentException ignored) {
                        // Not an island data file.
                        return;
                    }
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(path.toFile());
                    String leaderUuid = config.getString("party.leader-uuid", null);
                    if (leaderUuid != null && !leaderUuid.isBlank()) {
                        try {
                            UUID uuid = UUID.fromString(leaderUuid);
                            byLeader.put(uuid, islandKey);
                            byMember.put(uuid, islandKey);
                        } catch (IllegalArgumentException ignored) {
                            // Ignore invalid leader UUIDs.
                        }
                    }
                    ConfigurationSection membersSection = config.getConfigurationSection("party.members");
                    if (membersSection != null) {
                        for (String memberKey : membersSection.getKeys(false)) {
                            try {
                                byMember.put(UUID.fromString(memberKey), islandKey);
                            } catch (IllegalArgumentException ignored) {
                                // Ignore ancient name-keyed member entries.
                            }
                        }
                    }
                });
        } catch (Exception e) {
            throw new IllegalStateException("Unable to scan island data directory " + islandStorageDir, e);
        }
        return new IslandUuidIndex(byLeader, byMember);
    }

    private @Nullable IslandKey resolveLegacyCompletionOwner(
        @NotNull Path legacyFile,
        @NotNull IslandUuidIndex islandIndex,
        @NotNull CompletionScanResult result,
        @NotNull MigrationCounters counters
    ) {
        String fileName = legacyFile.getFileName().toString();
        String basename = fileName.substring(0, fileName.length() - 4);
        try {
            IslandKey islandKey = IslandKey.fromIslandName(basename);
            if (legacyPlayerSharing) {
                // Island-named files are stale data from a former challengeSharing=island
                // setup; the player-sharing runtime never read them.
                counters.residue.incrementAndGet();
                return null;
            }
            return islandKey;
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(basename);
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("Unable to resolve legacy challenge progress owner for " + fileName + ". Leaving file in place.");
            counters.unmapped.incrementAndGet();
            return null;
        }
        if (legacyPlayerSharing) {
            // Per-player progress was live data: attach it to the player's island. The player's
            // own recorded island coordinates are authoritative (the source the previous version
            // keyed progress by); island membership entries are the fallback for missing player files.
            IslandKey islandKey = resolvePlayerModeOwner(uuid, islandIndex);
            if (islandKey == null) {
                plugin.getLogger().warning("No island found for legacy per-player challenge progress " + fileName + ". Leaving file in place.");
                counters.unmapped.incrementAndGet();
                return null;
            }
            result.players().add(uuid);
            return islandKey;
        }
        // Under island sharing the previous version only promoted the leader's per-player file,
        // and only when no island-named file existed; everything else is stale data from a
        // former challengeSharing=player setup.
        IslandKey islandKey = islandIndex.byLeader().get(uuid);
        if (islandKey == null || Files.isRegularFile(legacyStorageDir.resolve(islandKey.value() + ".yml"))) {
            counters.residue.incrementAndGet();
            return null;
        }
        return islandKey;
    }

    private @Nullable IslandKey resolvePlayerModeOwner(@NotNull UUID uuid, @NotNull IslandUuidIndex islandIndex) {
        Path playerFile = playerStorageDir.resolve(uuid + ".yml");
        if (Files.isRegularFile(playerFile)) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile.toFile());
            if (config.getInt("player.islandY", 0) != 0) {
                IslandKey byCoordinates = resolvePlayerIslandKey(config);
                if (byCoordinates != null) {
                    return byCoordinates;
                }
            }
        }
        return islandIndex.byMember().get(uuid);
    }

    private boolean isPlayerYmlSuperseded(
        @NotNull Path playerFile,
        @NotNull IslandKey islandKey,
        @NotNull CompletionScanResult completionFiles
    ) {
        if (!legacyPlayerSharing) {
            return completionFiles.islands().contains(islandKey);
        }
        String fileName = playerFile.getFileName().toString();
        try {
            UUID playerUuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
            return completionFiles.players().contains(playerUuid);
        } catch (IllegalArgumentException ignored) {
            // Name-keyed player files predate per-player completion files entirely.
            return false;
        }
    }

    private @Nullable IslandKey resolvePlayerIslandKey(@NotNull YamlConfiguration config) {
        int islandX = config.getInt("player.islandX", 0);
        int islandZ = config.getInt("player.islandZ", 0);
        try {
            return IslandKey.fromIslandName(islandX + "," + islandZ);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private @NotNull Map<ChallengeId, ChallengeCompletion> loadLegacyFile(@NotNull File configFile) {
        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(configFile);
        if (fileConfiguration.getRoot() == null) {
            return Collections.emptyMap();
        }
        return loadFromConfiguration(fileConfiguration.getRoot());
    }

    private @NotNull Map<ChallengeId, ChallengeCompletion> loadFromConfiguration(ConfigurationSection root) {
        Map<ChallengeId, ChallengeCompletion> challengeMap = new HashMap<>();
        if (root != null) {
            for (String challengeName : root.getKeys(false)) {
                long firstCompleted = root.getLong(challengeName + ".firstCompleted", 0);
                Instant firstCompletedDuration = firstCompleted > 0 ? Instant.ofEpochMilli(firstCompleted) : null;
                ChallengeId challengeId = ChallengeId.of(challengeName);
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

    private void mergeProgress(@NotNull Map<ChallengeId, ChallengeCompletion> target, @NotNull Map<ChallengeId, ChallengeCompletion> incoming) {
        for (Map.Entry<ChallengeId, ChallengeCompletion> entry : incoming.entrySet()) {
            ChallengeCompletion existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), entry.getValue());
                continue;
            }
            target.put(entry.getKey(), mergeCompletion(entry.getKey(), existing, entry.getValue()));
        }
    }

    private ChallengeCompletion mergeCompletion(
        @NotNull ChallengeId challengeKey,
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
