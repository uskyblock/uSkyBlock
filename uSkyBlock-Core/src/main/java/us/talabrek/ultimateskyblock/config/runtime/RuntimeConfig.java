package us.talabrek.ultimateskyblock.config.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record RuntimeConfig(
    @NotNull Locale locale,
    @NotNull General general,
    @NotNull Island island,
    @NotNull Extras extras,
    @NotNull Nether nether,
    @NotNull Advanced advanced,
    @NotNull Party party,
    @NotNull PluginUpdates pluginUpdates,
    @NotNull Spawning spawning,
    @NotNull Map<String, IslandScheme> islandSchemes,
    @NotNull Map<String, Boolean> confirmations
) {
    public boolean confirmationRequired(@NotNull String command, boolean defaultValue) {
        return confirmations.getOrDefault(command, defaultValue);
    }

    public @Nullable IslandScheme islandScheme(@NotNull String schemeName) {
        return islandSchemes.get(schemeName);
    }

    public record General(
        int maxPartySize,
        @NotNull String worldName,
        int cooldownInfo,
        @NotNull Duration cooldownRestart,
        @NotNull Duration biomeChange,
        @NotNull String defaultBiomeKey,
        @NotNull String defaultNetherBiomeKey,
        int spawnSize,
        int maxSpam
    ) {
    }

    public record Island(
        int distance,
        int height,
        boolean removeCreaturesByTeleport,
        int protectionRange,
        int radius,
        @NotNull List<String> chestItemSpecs,
        boolean addExtraItems,
        @NotNull Set<String> extraPermissions,
        boolean allowIslandLock,
        boolean useIslandLevel,
        boolean useTopTen,
        @NotNull String defaultScheme,
        @NotNull Duration topTenTimeout,
        boolean allowPvP,
        @NotNull Duration teleportDelay,
        double teleportCancelDistance,
        boolean spawnLimitsEnabled
    ) {
    }

    public record Extras(
        boolean sendToSpawn,
        boolean respawnAtIsland,
        boolean obsidianToLava
    ) {
    }

    public record Nether(
        boolean enabled,
        int lavaLevel,
        int height
    ) {
    }

    public record Advanced(
        @NotNull Duration confirmTimeout
    ) {
    }

    public record Party(
        @NotNull Duration inviteTimeout
    ) {
    }

    public record PluginUpdates(
        @NotNull String branch
    ) {
    }

    public record Spawning(
        @NotNull Guardians guardians
    ) {
    }

    public record Guardians(
        boolean enabled,
        int maxPerIsland,
        double spawnChance
    ) {
    }

    public record IslandScheme(
        boolean enabled,
        @Nullable String schematic,
        @Nullable String netherSchematic
    ) {
    }
}
