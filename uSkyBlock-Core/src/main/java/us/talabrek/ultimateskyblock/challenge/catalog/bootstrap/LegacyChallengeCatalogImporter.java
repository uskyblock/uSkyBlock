package us.talabrek.ultimateskyblock.challenge.catalog.bootstrap;

import dk.lockfuglsang.minecraft.file.FileUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletionLogic;
import us.talabrek.ultimateskyblock.config.PluginConfig;
import us.talabrek.ultimateskyblock.imports.BlockRequirementConverter;
import us.talabrek.ultimateskyblock.imports.ItemComponentConverter;
import us.talabrek.ultimateskyblock.util.BackupFileUtil;
import us.talabrek.ultimateskyblock.util.MetaUtil;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class LegacyChallengeCatalogImporter {
    private static final int TARGET_SCHEMA_VERSION = 1;
    private static final int ITEM_COMPONENT_VERSION = 106;
    private static final int BLOCK_REQUIREMENT_VERSION = 108;
    private static final String DEFAULT_LOCKED_DISPLAY_ITEM = "minecraft:barrier";
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path pluginDataDir;
    private final Logger logger;
    private final PluginConfig pluginConfig;
    private final ItemComponentConverter itemComponentConverter;
    private final BlockRequirementConverter blockRequirementConverter;

    public LegacyChallengeCatalogImporter(
        @NotNull Path pluginDataDir,
        @NotNull Logger logger,
        @NotNull PluginConfig pluginConfig
    ) {
        this.pluginDataDir = pluginDataDir;
        this.logger = logger;
        this.pluginConfig = pluginConfig;
        this.itemComponentConverter = new ItemComponentConverter(logger);
        this.blockRequirementConverter = new BlockRequirementConverter(logger);
    }

    public boolean needsImport(@NotNull YamlConfiguration config) {
        return !config.contains("schemaVersion");
    }

    public void importLegacyFile(@NotNull Path challengesPath) {
        YamlConfiguration legacyConfig = YamlConfiguration.loadConfiguration(challengesPath.toFile());
        if (!needsImport(legacyConfig)) {
            return;
        }

        logger.info("Importing legacy challenges.yml into the new challenge catalog format.");
        applyLegacyMigrations(legacyConfig);
        migrateRuntimeSettings(legacyConfig);
        YamlConfiguration newConfig = convertLegacyConfig(legacyConfig);

        try {
            BackupFileUtil.copyToBackup(pluginDataDir, challengesPath, backupFileName());
            saveAtomically(challengesPath, newConfig);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to import legacy challenges.yml", e);
        }
    }

    private void applyLegacyMigrations(@NotNull YamlConfiguration legacyConfig) {
        int version = legacyConfig.getInt("version", 0);
        if (version <= ITEM_COMPONENT_VERSION) {
            itemComponentConverter.convertChallengeItemsInMemory(legacyConfig);
            legacyConfig.set("version", ITEM_COMPONENT_VERSION + 1);
        }
        if (legacyConfig.getInt("version", 0) < BLOCK_REQUIREMENT_VERSION) {
            blockRequirementConverter.convertBlockRequirementsInMemory(legacyConfig);
            legacyConfig.set("version", BLOCK_REQUIREMENT_VERSION);
        }
    }

    private void migrateRuntimeSettings(@NotNull YamlConfiguration legacyConfig) {
        pluginConfig.getYamlConfig().set("options.challenges.enabled", legacyConfig.getBoolean("allowChallenges", true));
        pluginConfig.getYamlConfig().set("options.challenges.reset-on-create", legacyConfig.getBoolean("resetChallengesOnCreate", true));
        pluginConfig.getYamlConfig().set("options.challenges.enable-economy-rewards", legacyConfig.getBoolean("enableEconomyPlugin", true));
        pluginConfig.getYamlConfig().set("options.challenges.broadcast.enabled", legacyConfig.getBoolean("broadcastCompletion", true));
        pluginConfig.getYamlConfig().set("options.challenges.broadcast.prefix", Objects.toString(legacyConfig.getString("broadcastText"), ""));
        if ("player".equalsIgnoreCase(legacyConfig.getString("challengeSharing", "island"))) {
            logger.warning("Legacy challengeSharing=player is deprecated. Challenge progress remains island-owned after migration.");
            // The one-shot SQLite progress migration needs this flag to interpret legacy per-player
            // data correctly, and this import destroys the original challenges.yml that carried it.
            pluginConfig.getYamlConfig().set(ChallengeCompletionLogic.LEGACY_PLAYER_SHARING_CONFIG_KEY, true);
        }
        try {
            pluginConfig.save();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist config.yml after migrating challenge settings", e);
        }
    }

    private @NotNull YamlConfiguration convertLegacyConfig(@NotNull YamlConfiguration legacyConfig) {
        YamlConfiguration newConfig = new YamlConfiguration();
        newConfig.set("schemaVersion", TARGET_SCHEMA_VERSION);

        ConfigurationSection ranksSection = legacyConfig.getConfigurationSection("ranks");
        if (ranksSection == null) {
            throw new IllegalArgumentException("Legacy challenges.yml is missing the 'ranks' section");
        }

        boolean requiresPreviousRank = legacyConfig.getBoolean("requiresPreviousRank", true);
        int defaultRankLeeway = legacyConfig.getInt("rankLeeway", 1);
        long defaultResetInHours = legacyConfig.getLong("defaultResetInHours", 144L);
        int defaultRadius = legacyConfig.getInt("radius", 10);
        int defaultRepeatLimit = legacyConfig.getInt("repeatLimit", 0);
        String rootLockedDisplayItem = legacyConfig.getString("lockedDisplayItem", DEFAULT_LOCKED_DISPLAY_ITEM);

        ConfigurationSection outputRanks = newConfig.createSection("ranks");
        @Nullable LegacyRankState previousRank = null;
        for (String rankKey : ranksSection.getKeys(false)) {
            ConfigurationSection legacyRank = requireSection(ranksSection, rankKey, "ranks");
            ConfigurationSection outputRank = outputRanks.createSection(rankKey);
            ConfigurationSection display = outputRank.createSection("display");
            display.set("name", convertLegacyText(legacyRank.getString("name", rankKey)));
            String rankDescription = convertLegacyText(legacyRank.getString("description", ""));
            if (!rankDescription.isBlank()) {
                display.set("description", rankDescription);
            }
            outputRank.set("lockedDisplayItem", normalizeItemSpec(legacyRank.getString("displayItem", rootLockedDisplayItem)));

            List<Map<String, Object>> rankUnlock = new ArrayList<>();
            if (previousRank != null) {
                ConfigurationSection requires = legacyRank.getConfigurationSection("requires");
                Integer explicitLeeway = requires != null && requires.contains("rankLeeway")
                    ? requires.getInt("rankLeeway")
                    : null;
                if (explicitLeeway != null || requiresPreviousRank) {
                    int leeway = explicitLeeway != null ? explicitLeeway : defaultRankLeeway;
                    rankUnlock.add(Map.of(
                        "type", "completed-rank",
                        "rank", previousRank.rankKey(),
                        "minimumCompletedChallenges", Math.max(0, previousRank.challengeCount() - leeway)
                    ));
                }
            }
            List<String> requiredRankChallenges = legacyRank.getStringList("requires.challenges");
            if (!requiredRankChallenges.isEmpty()) {
                rankUnlock.add(Map.of(
                    "type", "completed-challenges",
                    "challenges", normalizeChallengeReferences(requiredRankChallenges)
                ));
            }
            if (!rankUnlock.isEmpty()) {
                outputRank.set("unlock", rankUnlock);
            }

            ConfigurationSection outputChallenges = outputRank.createSection("challenges");
            ConfigurationSection legacyChallenges = requireSection(legacyRank, "challenges", "ranks." + rankKey);
            int activeChallengeCount = 0;
            for (String challengeKey : legacyChallenges.getKeys(false)) {
                ConfigurationSection legacyChallenge = requireSection(legacyChallenges, challengeKey, "ranks." + rankKey + ".challenges");
                if (legacyChallenge.getBoolean("disabled", false)) {
                    continue;
                }
                activeChallengeCount++;
                ConfigurationSection outputChallenge = outputChallenges.createSection(challengeKey);
                mapChallenge(legacyConfig, legacyRank, legacyChallenge, challengeKey, outputChallenge, defaultResetInHours, defaultRadius,
                    defaultRepeatLimit, rootLockedDisplayItem);
            }

            previousRank = new LegacyRankState(rankKey, activeChallengeCount);
        }

        return newConfig;
    }

    private void mapChallenge(
        @NotNull YamlConfiguration legacyRoot,
        @NotNull ConfigurationSection legacyRank,
        @NotNull ConfigurationSection legacyChallenge,
        @NotNull String challengeKey,
        @NotNull ConfigurationSection outputChallenge,
        long defaultResetInHours,
        int defaultRadius,
        int defaultRepeatLimit,
        @NotNull String rootLockedDisplayItem
    ) {
        ConfigurationSection display = outputChallenge.createSection("display");
        display.set("name", convertLegacyText(legacyChallenge.getString("name", challengeKey)));
        String description = convertLegacyText(legacyChallenge.getString("description", ""));
        if (!description.isBlank()) {
            display.set("description", description);
        }
        display.set("item", normalizeItemSpec(legacyChallenge.getString("displayItem", "minecraft:stone")));

        String challengeType = legacyChallenge.getString("type", "onPlayer");
        String lockedDisplayItem = legacyChallenge.getString("lockedDisplayItem",
            legacyRoot.getString(challengeType + ".lockedDisplayItem", rootLockedDisplayItem));
        if (lockedDisplayItem != null && !lockedDisplayItem.isBlank()) {
            outputChallenge.set("lockedDisplayItem", normalizeItemSpec(lockedDisplayItem));
        }

        List<Map<String, Object>> unlock = new ArrayList<>();
        List<String> requiredChallenges = legacyChallenge.getStringList("requiredChallenges");
        if (!requiredChallenges.isEmpty()) {
            unlock.add(Map.of("type", "completed-challenges", "challenges", normalizeChallengeReferences(requiredChallenges)));
        }
        if (!unlock.isEmpty()) {
            outputChallenge.set("unlock", unlock);
        }

        List<Map<String, Object>> completion = mapCompletionRequirements(legacyChallenge, challengeType, defaultRadius);
        if (!completion.isEmpty()) {
            outputChallenge.set("complete", completion);
        }

        ConfigurationSection properties = outputChallenge.createSection("properties");
        properties.set("consumeItemsOnCompletion", legacyChallenge.getBoolean("takeItems", true));

        ConfigurationSection repeat = outputChallenge.createSection("repeat");
        boolean repeatable = legacyChallenge.getBoolean("repeatable", false) || legacyChallenge.isConfigurationSection("repeatReward");
        repeat.set("enabled", repeatable);
        long resetInHours = legacyChallenge.getLong("resetInHours", legacyRank.getLong("resetInHours", defaultResetInHours));
        repeat.set("resetWindow", resetInHours + "h");
        repeat.set("limit", legacyChallenge.getInt("repeatLimit", defaultRepeatLimit));

        ConfigurationSection rewards = outputChallenge.createSection("rewards");
        rewards.set("first", mapRewards(legacyChallenge.getConfigurationSection("reward")));
        rewards.set("repeat", mapRewards(legacyChallenge.getConfigurationSection("repeatReward")));
    }

    private @NotNull List<Map<String, Object>> mapCompletionRequirements(
        @NotNull ConfigurationSection legacyChallenge,
        @NotNull String challengeType,
        int defaultRadius
    ) {
        List<Map<String, Object>> completion = new ArrayList<>();
        switch (challengeType.toLowerCase(Locale.ROOT)) {
            case "onplayer" -> {
                List<String> requiredItems = legacyChallenge.getStringList("requiredItems");
                if (!requiredItems.isEmpty()) {
                    completion.add(Map.of("type", "inventory-items", "items", mapItemRequirements(requiredItems)));
                }
            }
            case "onisland" -> {
                List<String> requiredBlocks = legacyChallenge.getStringList("requiredBlocks");
                if (!requiredBlocks.isEmpty()) {
                    completion.add(Map.of(
                        "type", "island-blocks",
                        "radius", legacyChallenge.getInt("radius", defaultRadius),
                        "blocks", mapBlockRequirements(requiredBlocks)
                    ));
                }
                List<String> requiredEntities = legacyChallenge.getStringList("requiredEntities");
                if (!requiredEntities.isEmpty()) {
                    completion.add(Map.of(
                        "type", "entity-presence",
                        "radius", legacyChallenge.getInt("radius", defaultRadius),
                        "entities", mapEntityRequirements(requiredEntities)
                    ));
                }
            }
            case "islandlevel" -> completion.add(Map.of(
                "type", "island-level",
                "minimum", legacyChallenge.getDouble("requiredLevel", 0d)
            ));
            default -> logger.warning("Unknown legacy challenge type '" + challengeType + "'; skipping completion requirements.");
        }
        return completion;
    }

    private @NotNull List<Map<String, Object>> mapRewards(@Nullable ConfigurationSection rewardSection) {
        if (rewardSection == null) {
            return List.of();
        }
        List<Map<String, Object>> rewards = new ArrayList<>();
        List<String> items = rewardSection.getStringList("items");
        if (!items.isEmpty()) {
            rewards.add(Map.of("type", "item", "items", mapRewardItems(items)));
        }
        String permission = rewardSection.getString("permission");
        if (permission != null && !permission.isBlank()) {
            rewards.add(Map.of("type", "permission", "permissions", List.of(permission)));
        }
        if (rewardSection.getInt("currency", 0) > 0) {
            rewards.add(Map.of("type", "economy", "amount", rewardSection.getInt("currency")));
        }
        if (rewardSection.getInt("xp", 0) > 0) {
            rewards.add(Map.of("type", "experience", "amount", rewardSection.getInt("xp")));
        }
        List<Map<String, Object>> commands = mapRewardCommands(rewardSection.getStringList("commands"));
        if (!commands.isEmpty()) {
            rewards.add(Map.of("type", "command", "commands", commands));
        }
        return rewards;
    }

    private @NotNull List<Map<String, Object>> mapRewardCommands(@NotNull List<String> commands) {
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }
            String execution = "player";
            String value = command;
            if (command.regionMatches(true, 0, "op:", 0, 3)) {
                execution = "op";
                value = command.substring(3);
            } else if (command.regionMatches(true, 0, "console:", 0, 8)) {
                execution = "console";
                value = command.substring(8);
            }
            mapped.add(Map.of("execution", execution, "command", value.trim()));
        }
        return mapped;
    }

    private @NotNull List<Map<String, Object>> mapRewardItems(@NotNull List<String> items) {
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (String item : items) {
            if (item == null || item.isBlank()) {
                continue;
            }
            double probability = 1.0d;
            String spec = item.trim();
            if (spec.startsWith("{p=")) {
                int end = spec.indexOf('}');
                if (end > 0) {
                    probability = Double.parseDouble(spec.substring(3, end));
                    spec = spec.substring(end + 1);
                }
            }
            int lastColon = spec.lastIndexOf(':');
            if (lastColon < 0) {
                continue;
            }
            int amount = Integer.parseInt(spec.substring(lastColon + 1));
            String itemSpec = spec.substring(0, lastColon);
            mapped.add(Map.of(
                "item", normalizeItemSpec(itemSpec),
                "amount", amount,
                "probability", probability
            ));
        }
        return mapped;
    }

    private @NotNull List<Map<String, Object>> mapItemRequirements(@NotNull List<String> requirements) {
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (String requirement : requirements) {
            int amountSeparator = requirement.lastIndexOf(':');
            if (amountSeparator < 0) {
                continue;
            }
            String base = requirement.substring(0, amountSeparator);
            String remainder = requirement.substring(amountSeparator + 1);
            String amountText = remainder;
            String progressionOperator = "none";
            double progressionIncrement = 1.0d;
            int progressionSeparator = remainder.indexOf(';');
            if (progressionSeparator >= 0) {
                amountText = remainder.substring(0, progressionSeparator);
                String progression = remainder.substring(progressionSeparator + 1);
                if (progression.length() >= 2) {
                    // The pre-1.20.6 plugin treated '^' as a multiply alias; '/' is the 3.x divide symbol.
                    progressionOperator = switch (progression.charAt(0)) {
                        case '+' -> "add";
                        case '-' -> "subtract";
                        case '*', '^' -> "multiply";
                        case '/' -> "divide";
                        default -> "none";
                    };
                    progressionIncrement = Double.parseDouble(progression.substring(1));
                }
            }
            mapped.add(Map.of(
                "item", normalizeItemSpec(base),
                "amount", Integer.parseInt(amountText),
                "progression", Map.of("operator", progressionOperator, "increment", progressionIncrement)
            ));
        }
        return mapped;
    }

    private @NotNull List<Map<String, Object>> mapBlockRequirements(@NotNull List<String> requirements) {
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (String requirement : requirements) {
            int amountSeparator = requirement.lastIndexOf(':');
            if (amountSeparator < 0) {
                continue;
            }
            mapped.add(Map.of(
                "block", normalizeItemSpec(requirement.substring(0, amountSeparator)),
                "amount", Integer.parseInt(requirement.substring(amountSeparator + 1))
            ));
        }
        return mapped;
    }

    private @NotNull List<Map<String, Object>> mapEntityRequirements(@NotNull List<String> requirements) {
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (String requirement : requirements) {
            String[] countSplit = requirement.split(":(?=[0-9]+$)", 2);
            String entityPart = countSplit[0];
            int count = countSplit.length > 1 ? Integer.parseInt(countSplit[1]) : 1;
            int metadataIndex = entityPart.indexOf(":{");
            String entityType = metadataIndex >= 0 ? entityPart.substring(0, metadataIndex) : entityPart;
            Map<String, Object> metadata = metadataIndex >= 0
                ? MetaUtil.createMap(entityPart.substring(metadataIndex + 1))
                : Map.of();
            mapped.add(Map.of(
                "entity", entityType.toLowerCase(Locale.ROOT),
                "count", count,
                "metadata", metadata
            ));
        }
        return mapped;
    }

    private static @NotNull String convertLegacyText(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.contains("<") && value.contains(">") && !value.contains("&") && !value.contains("§")) {
            return value;
        }
        String normalized = value.indexOf('§') >= 0 ? value : value.replace('§', '&');
        Component component = normalized.indexOf('§') >= 0
            ? LegacyComponentSerializer.legacySection().deserialize(normalized)
            : LegacyComponentSerializer.legacyAmpersand().deserialize(normalized);
        return MiniMessage.miniMessage().serialize(component);
    }

    private static @NotNull String normalizeItemSpec(@Nullable String value) {
        return value == null || value.isBlank() ? DEFAULT_LOCKED_DISPLAY_ITEM : value;
    }

    private static @NotNull List<String> normalizeChallengeReferences(@NotNull List<String> challengeReferences) {
        return challengeReferences.stream()
            .map(reference -> {
                int index = reference.indexOf(':');
                return index >= 0 ? reference.substring(0, index) : reference;
            })
            .toList();
    }

    private static @NotNull ConfigurationSection requireSection(
        @NotNull ConfigurationSection parent,
        @NotNull String key,
        @NotNull String path
    ) {
        ConfigurationSection section = parent.getConfigurationSection(key);
        if (section == null) {
            throw new IllegalArgumentException("Missing required section '" + path + "." + key + "'");
        }
        return section;
    }

    private @NotNull String backupFileName() {
        return "challenges/challenges-" + LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT) + ".yml";
    }

    private void saveAtomically(@NotNull Path target, @NotNull YamlConfiguration config) throws IOException {
        Path tempFile = Files.createTempFile(pluginDataDir, "challenge-catalog-", ".yml");
        try {
            config.save(tempFile.toFile());
            try {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void ensureChallengeFileExists() {
        if (Files.exists(pluginDataDir.resolve(ChallengeCatalogLoader.CHALLENGES_FILE_NAME))) {
            return;
        }
        FileUtil.getYmlConfiguration(ChallengeCatalogLoader.CHALLENGES_FILE_NAME);
    }

    private record LegacyRankState(String rankKey, int challengeCount) {
    }
}
