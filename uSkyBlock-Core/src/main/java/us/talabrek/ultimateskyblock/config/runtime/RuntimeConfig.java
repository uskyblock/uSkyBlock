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
    @NotNull Restart restart,
    @NotNull Advanced advanced,
    @NotNull Async async,
    @NotNull AsyncWorldEdit asyncWorldEdit,
    @NotNull Party party,
    @NotNull PluginUpdates pluginUpdates,
    @NotNull Spawning spawning,
    @NotNull Placeholder placeholder,
    @NotNull ToolMenu toolMenu,
    @NotNull Signs signs,
    @NotNull WorldGuard worldGuard,
    @NotNull Importer importer,
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
        @NotNull Duration reservationTimeout,
        boolean allowPvP,
        @NotNull Duration teleportDelay,
        double teleportCancelDistance,
        int autoRefreshScore,
        boolean topTenShowMembers,
        int logSize,
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
        boolean visitorItemDrops,
        boolean protectLava,
        boolean visitorFallProtected,
        boolean visitorFireProtected,
        boolean visitorMonsterProtected,
        boolean visitorWarnOnWarp,
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

    public record Restart(
        boolean clearInventory,
        boolean clearPerms,
        boolean clearArmor,
        boolean clearEnderChest,
        boolean clearCurrency,
        boolean teleportWhenReady,
        @NotNull Duration teleportDelay,
        @NotNull List<String> extraCommands
    ) {
    }

    public record Advanced(
        @NotNull Duration confirmTimeout,
        boolean useDisplayNames,
        double topTenCutoff,
        boolean manageSpawn,
        @NotNull String playerCacheSpec,
        @NotNull String islandCacheSpec,
        @NotNull String placeholderCacheSpec,
        @NotNull String completionCacheSpec,
        @NotNull Duration islandSaveEvery,
        @NotNull Duration playerSaveEvery,
        @NotNull String chunkGenerator,
        int chunkRegenSpeed,
        @NotNull Duration feedbackEvery,
        double purgeLevel,
        @NotNull Duration purgeTimeout,
        @Nullable String debugLevel,
        @NotNull PlayerDb playerDb
    ) {
    }

    public record PlayerDb(
        @NotNull String storage,
        @NotNull String nameCacheSpec,
        @NotNull String uuidCacheSpec,
        @NotNull Duration saveDelay
    ) {
    }

    public record Async(
        @NotNull Duration maxIterationTime,
        long maxConsecutiveTicks,
        @NotNull Duration yieldDelay
    ) {
    }

    public record AsyncWorldEdit(
        boolean enabled,
        @NotNull Duration heartBeat,
        @NotNull Duration timeout
    ) {
    }

    public record Party(
        @NotNull Duration inviteTimeout,
        @NotNull String chatFormat,
        @NotNull List<String> joinCommands,
        @NotNull List<String> leaveCommands,
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
        boolean enabled,
        @NotNull String toolSpec,
        @NotNull Map<String, String> commands
    ) {
    }

    public record Signs(
        boolean enabled
    ) {
    }

    public record WorldGuard(
        boolean entryMessage,
        boolean exitMessage
    ) {
    }

    public record Importer(
        double progressEveryPct,
        @NotNull Duration progressEvery
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
