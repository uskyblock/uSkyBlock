package us.talabrek.ultimateskyblock.config.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record RuntimeConfig(
    @NotNull String configuredLanguage,
    @NotNull Locale locale,
    @NotNull General general,
    @NotNull Island island,
    @NotNull Extras extras,
    @NotNull Protection protection,
    @NotNull Nether nether,
    @NotNull Advanced advanced,
    @NotNull Async async,
    @NotNull Party party,
    @NotNull PluginUpdates pluginUpdates,
    @NotNull Spawning spawning,
    @NotNull Placeholder placeholder,
    @NotNull ToolMenu toolMenu,
    @NotNull Signs signs,
    @NotNull Map<String, IslandScheme> islandSchemes,
    @NotNull Map<Integer, ExtraMenu> extraMenus,
    @NotNull Map<String, PerkSpec> donorPerks,
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
        @NotNull Map<String, List<String>> extraPermissionItems,
        boolean allowIslandLock,
        boolean useIslandLevel,
        boolean useTopTen,
        @NotNull String defaultScheme,
        @NotNull Duration topTenTimeout,
        boolean allowPvP,
        @NotNull Duration teleportDelay,
        double teleportCancelDistance,
        int autoRefreshScore,
        boolean topTenShowMembers,
        boolean schemesEnabled,
        @NotNull String chatFormat,
        @NotNull SpawnLimits spawnLimits,
        @NotNull Map<String, Integer> blockLimits
    ) {
        public @NotNull Set<String> extraPermissions() {
            return extraPermissionItems.keySet();
        }
    }

    public record SpawnLimits(
        boolean enabled,
        int animals,
        int monsters,
        int villagers,
        int golems,
        int copperGolems
    ) {
    }

    public record Extras(
        boolean sendToSpawn,
        boolean respawnAtIsland,
        boolean obsidianToLava
    ) {
    }

    public record Protection(
        boolean enabled,
        boolean itemDrops,
        boolean blockBannedEntry
    ) {
    }

    public record Nether(
        boolean enabled,
        int lavaLevel,
        int height,
        @NotNull String chunkGenerator
    ) {
    }

    public record Advanced(
        @NotNull Duration confirmTimeout,
        boolean useDisplayNames,
        double topTenCutoff,
        boolean manageSpawn,
        @NotNull String playerCacheSpec,
        @NotNull String islandCacheSpec,
        @NotNull Duration islandSaveEvery,
        @NotNull Duration playerSaveEvery,
        @NotNull String chunkGenerator,
        @NotNull PlayerDb playerDb
    ) {
    }

    public record PlayerDb(
        @NotNull String storage,
        @NotNull String nameCacheSpec,
        @NotNull String uuidCacheSpec
    ) {
    }

    public record Async(
        @NotNull Duration maxIterationTime,
        long maxConsecutiveTicks,
        @NotNull Duration yieldDelay
    ) {
    }

    public record Party(
        @NotNull Duration inviteTimeout,
        @NotNull String chatFormat,
        @NotNull Map<String, Integer> maxPartyPermissions
    ) {
    }

    public record PluginUpdates(
        boolean check,
        @NotNull String branch
    ) {
    }

    public record Spawning(
        @NotNull Guardians guardians,
        @NotNull Phantoms phantoms
    ) {
    }

    public record Guardians(
        boolean enabled,
        int maxPerIsland,
        double spawnChance
    ) {
    }

    public record Phantoms(
        boolean overworld,
        boolean nether
    ) {
    }

    public record Placeholder(
        boolean chatPlaceholder,
        boolean serverCommandPlaceholder,
        boolean mvdwPlaceholderApi
    ) {
    }

    public record ToolMenu(
        boolean enabled
    ) {
    }

    public record Signs(
        boolean enabled
    ) {
    }

    public record IslandScheme(
        boolean enabled,
        @Nullable String schematic,
        @Nullable String netherSchematic,
        @NotNull String permission,
        @Nullable String description,
        @NotNull String displayItem,
        int index,
        @NotNull List<String> extraItemSpecs,
        int maxPartySize,
        int animals,
        int monsters,
        int villagers,
        int golems,
        int copperGolems,
        double scoreMultiply,
        double scoreOffset
    ) {
    }

    public record ExtraMenu(
        @NotNull String title,
        @NotNull String displayItem,
        @NotNull List<String> lore,
        @NotNull List<String> commands
    ) {
    }

    public record PerkSpec(
        @NotNull List<String> extraItemSpecs,
        int maxPartySize,
        int animals,
        int monsters,
        int villagers,
        int golems,
        int copperGolems,
        double rewardBonus,
        double hungerReduction,
        @NotNull List<String> schematics
    ) {
    }
}
