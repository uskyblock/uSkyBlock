package us.talabrek.ultimateskyblock.challenge.catalog.yaml;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.util.BlockRequirement;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalogDiagnostic;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalogValidator;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeProperties;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards;
import us.talabrek.ultimateskyblock.challenge.catalog.DisplaySpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDisplaySpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;
import us.talabrek.ultimateskyblock.challenge.catalog.RepeatPolicy;
import us.talabrek.ultimateskyblock.challenge.catalog.RewardBundle;
import us.talabrek.ultimateskyblock.challenge.catalog.TextSpec;
import us.talabrek.ultimateskyblock.config.ConfigDuration;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.gameobject.ItemStackAmountProbabilitySpec;
import us.talabrek.ultimateskyblock.gameobject.ItemStackAmountSpec;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Singleton
public class ChallengeCatalogYamlParser {
    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private static final String DEFAULT_LOCKED_DISPLAY_ITEM = "minecraft:barrier";
    private static final String DEFAULT_CHALLENGE_DISPLAY_ITEM = "minecraft:stone";

    private final GameObjectFactory gameObjects;
    private final ChallengeCatalogValidator validator;

    @Inject
    public ChallengeCatalogYamlParser(@NotNull GameObjectFactory gameObjects) {
        this(gameObjects, new ChallengeCatalogValidator());
    }

    ChallengeCatalogYamlParser(@NotNull GameObjectFactory gameObjects, @NotNull ChallengeCatalogValidator validator) {
        this.gameObjects = gameObjects;
        this.validator = validator;
    }

    public @NotNull ChallengeCatalogParseResult parse(@NotNull FileConfiguration config) {
        List<ChallengeCatalogDiagnostic> diagnostics = new ArrayList<>();
        warnUnknownKeys(config, "$", diagnostics, "schemaVersion", "ranks");

        int schemaVersion = config.getInt("schemaVersion", -1);
        if (schemaVersion == -1) {
            throw new IllegalArgumentException("Missing required root key '$.schemaVersion'");
        }
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported schema version: " + schemaVersion);
        }

        ConfigurationSection ranksSection = requiredSection(config, "ranks", "$");
        List<RankDefinition> ranks = new ArrayList<>();
        for (String rankKey : ranksSection.getKeys(false)) {
            ConfigurationSection rankSection = requiredSection(ranksSection, rankKey, "$.ranks");
            ranks.add(parseRank(rankKey, rankSection, "$.ranks." + rankKey, diagnostics));
        }

        ChallengeCatalog catalog = new ChallengeCatalog(ranks);
        diagnostics.addAll(validator.validate(catalog));
        return new ChallengeCatalogParseResult(catalog, diagnostics);
    }

    private RankDefinition parseRank(String rankKey, ConfigurationSection section, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        warnUnknownKeys(section, path, diagnostics, "display", "lockedDisplayItem", "unlock", "challenges");

        RankDisplaySpec display = parseRankDisplay(rankKey, section.getConfigurationSection("display"), path + ".display", diagnostics);
        ItemStackSpec lockedDisplayItem = parseRankLockedDisplayItem(section, path, diagnostics);
        List<ChallengeRequirements.RankUnlockRequirement> unlockRequirements = parseRankUnlockRequirements(section.getMapList("unlock"), path + ".unlock", diagnostics);

        List<ChallengeDefinition> challenges = new ArrayList<>();
        ConfigurationSection challengesSection = section.getConfigurationSection("challenges");
        if (challengesSection == null) {
            diagnostics.add(warn(path + ".challenges", "Missing challenges section, defaulting to an empty challenge list"));
        } else {
            for (String challengeKey : challengesSection.getKeys(false)) {
                ConfigurationSection challengeSection = requiredSection(challengesSection, challengeKey, path + ".challenges");
                challenges.add(parseChallenge(challengeKey, challengeSection, lockedDisplayItem, path + ".challenges." + challengeKey, diagnostics));
            }
        }

        return new RankDefinition(RankId.of(rankKey), display, lockedDisplayItem, unlockRequirements, challenges);
    }

    private ChallengeDefinition parseChallenge(
        String challengeKey,
        ConfigurationSection section,
        ItemStackSpec rankLockedDisplayItem,
        String path,
        List<ChallengeCatalogDiagnostic> diagnostics
    ) {
        warnUnknownKeys(section, path, diagnostics, "display", "lockedDisplayItem", "unlock", "complete", "properties", "repeat", "rewards");

        DisplaySpec display = parseChallengeDisplay(challengeKey, section.getConfigurationSection("display"), path + ".display", diagnostics);
        ItemStackSpec lockedDisplayItem = parseChallengeLockedDisplayItem(section, rankLockedDisplayItem, path, diagnostics);
        List<ChallengeRequirements.ChallengeUnlockRequirement> unlockRequirements = parseChallengeUnlockRequirements(section.getMapList("unlock"), path + ".unlock", diagnostics);
        List<ChallengeRequirements.CompletionRequirement> completionRequirements = parseCompletionRequirements(section.getMapList("complete"), path + ".complete", diagnostics);
        ChallengeProperties properties = parseProperties(section.getConfigurationSection("properties"), path + ".properties", diagnostics);
        RepeatPolicy repeatPolicy = parseRepeatPolicy(section.getConfigurationSection("repeat"), path + ".repeat", diagnostics);
        RewardBundle firstReward = parseRewardBundle(section.getMapList("rewards.first"), path + ".rewards.first", diagnostics);
        RewardBundle repeatReward = parseRewardBundle(section.getMapList("rewards.repeat"), path + ".rewards.repeat", diagnostics);

        return new ChallengeDefinition(
            ChallengeId.of(challengeKey),
            display,
            lockedDisplayItem,
            unlockRequirements,
            completionRequirements,
            properties,
            repeatPolicy,
            firstReward,
            repeatReward
        );
    }

    private DisplaySpec parseChallengeDisplay(
        String challengeKey,
        @Nullable ConfigurationSection section,
        String path,
        List<ChallengeCatalogDiagnostic> diagnostics
    ) {
        if (section == null) {
            diagnostics.add(warn(path, "Missing display section, defaulting challenge display fields"));
            return new DisplaySpec(TextSpec.miniMessage(challengeKey), TextSpec.empty(), gameObjects.itemStack(DEFAULT_CHALLENGE_DISPLAY_ITEM));
        }
        warnUnknownKeys(section, path, diagnostics, "name", "description", "item");
        String name = optionalString(section, "name").orElseGet(() -> {
            diagnostics.add(warn(path + ".name", "Missing name, defaulting to challenge key"));
            return challengeKey;
        });
        String itemSpec = optionalString(section, "item").orElseGet(() -> {
            diagnostics.add(warn(path + ".item", "Missing item, defaulting to '" + DEFAULT_CHALLENGE_DISPLAY_ITEM + "'"));
            return DEFAULT_CHALLENGE_DISPLAY_ITEM;
        });
        String description = section.getString("description", "");
        return new DisplaySpec(
            TextSpec.miniMessage(name),
            description.isEmpty() ? TextSpec.empty() : TextSpec.miniMessage(description),
            gameObjects.itemStack(itemSpec)
        );
    }

    private RankDisplaySpec parseRankDisplay(
        String rankKey,
        @Nullable ConfigurationSection section,
        String path,
        List<ChallengeCatalogDiagnostic> diagnostics
    ) {
        if (section == null) {
            diagnostics.add(warn(path, "Missing display section, defaulting rank display fields"));
            return new RankDisplaySpec(TextSpec.miniMessage(rankKey), TextSpec.empty());
        }
        warnUnknownKeys(section, path, diagnostics, "name", "description");
        String name = optionalString(section, "name").orElseGet(() -> {
            diagnostics.add(warn(path + ".name", "Missing name, defaulting to rank key"));
            return rankKey;
        });
        String description = section.getString("description", "");
        return new RankDisplaySpec(
            TextSpec.miniMessage(name),
            description.isEmpty() ? TextSpec.empty() : TextSpec.miniMessage(description)
        );
    }

    private ItemStackSpec parseRankLockedDisplayItem(ConfigurationSection section, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        String rawLockedDisplayItem = optionalString(section, "lockedDisplayItem").orElse(null);
        if (rawLockedDisplayItem == null || rawLockedDisplayItem.isBlank()) {
            diagnostics.add(warn(path + ".lockedDisplayItem", "Missing locked display item, defaulting to '" + DEFAULT_LOCKED_DISPLAY_ITEM + "'"));
            rawLockedDisplayItem = DEFAULT_LOCKED_DISPLAY_ITEM;
        }
        return gameObjects.itemStack(rawLockedDisplayItem);
    }

    private ItemStackSpec parseChallengeLockedDisplayItem(
        ConfigurationSection section,
        ItemStackSpec rankLockedDisplayItem,
        String path,
        List<ChallengeCatalogDiagnostic> diagnostics
    ) {
        String rawLockedDisplayItem = optionalString(section, "lockedDisplayItem").orElse(null);
        if (rawLockedDisplayItem == null || rawLockedDisplayItem.isBlank()) {
            diagnostics.add(warn(path + ".lockedDisplayItem", "Missing locked display item, defaulting to the rank locked display item"));
            return rankLockedDisplayItem;
        }
        return gameObjects.itemStack(rawLockedDisplayItem);
    }

    private ChallengeProperties parseProperties(@Nullable ConfigurationSection section, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        if (section == null) {
            return new ChallengeProperties(true);
        }
        warnUnknownKeys(section, path, diagnostics, "consumeItemsOnCompletion");
        return new ChallengeProperties(section.getBoolean("consumeItemsOnCompletion", true));
    }

    private RepeatPolicy parseRepeatPolicy(@Nullable ConfigurationSection section, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        if (section == null) {
            return new RepeatPolicy(false, Duration.ZERO, 0);
        }
        warnUnknownKeys(section, path, diagnostics, "enabled", "resetWindow", "limit");
        boolean enabled = section.getBoolean("enabled", false);
        Duration resetWindow = parseDuration(section.getString("resetWindow", "0s"), path + ".resetWindow");
        int limit = section.getInt("limit", 0);
        if (limit < 0) {
            throw new IllegalArgumentException("Invalid negative repeat limit at " + path + ".limit");
        }
        return new RepeatPolicy(enabled, resetWindow, limit);
    }

    private RewardBundle parseRewardBundle(List<Map<?, ?>> entries, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        if (entries == null || entries.isEmpty()) {
            return RewardBundle.empty();
        }
        List<ChallengeRewards.RewardAction> actions = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            String itemPath = path + "[" + i + "]";
            Map<String, Object> entry = normalizeMap(entries.get(i), itemPath);
            String type = requiredString(entry, "type", itemPath).toLowerCase(Locale.ROOT);
            switch (type) {
                case "item" -> actions.add(parseItemReward(entry, itemPath, diagnostics));
                case "economy" -> actions.add(new ChallengeRewards.EconomyReward(requiredInt(entry, "amount", itemPath)));
                case "experience" -> actions.add(new ChallengeRewards.ExperienceReward(requiredInt(entry, "amount", itemPath)));
                case "permission" -> actions.add(new ChallengeRewards.PermissionReward(requiredStringList(entry, "permissions", itemPath)));
                case "command" -> actions.add(parseCommandReward(entry, itemPath, diagnostics));
                default -> throw new IllegalArgumentException("Unsupported reward type '" + type + "' at " + itemPath);
            }
        }
        return new RewardBundle(actions);
    }

    private ChallengeRewards.ItemReward parseItemReward(Map<String, Object> entry, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        warnUnknownKeys(entry, path, diagnostics, "type", "items");
        List<Map<String, Object>> items = requiredMapList(entry, "items", path);
        List<ItemStackAmountProbabilitySpec> parsed = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            String itemPath = path + ".items[" + i + "]";
            Map<String, Object> item = items.get(i);
            warnUnknownKeys(item, itemPath, diagnostics, "item", "amount", "probability");
            ItemStackSpec itemSpec = gameObjects.itemStack(requiredString(item, "item", itemPath));
            int amount = requiredInt(item, "amount", itemPath);
            double probability = requiredDouble(item, "probability", itemPath, 1.0d);
            parsed.add(new ItemStackAmountProbabilitySpec(probability, new ItemStackAmountSpec(itemSpec, amount)));
        }
        return new ChallengeRewards.ItemReward(parsed);
    }

    private ChallengeRewards.CommandReward parseCommandReward(Map<String, Object> entry, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        warnUnknownKeys(entry, path, diagnostics, "type", "commands");
        List<Map<String, Object>> commands = requiredMapList(entry, "commands", path);
        List<ChallengeRewards.CommandSpec> parsed = new ArrayList<>();
        for (int i = 0; i < commands.size(); i++) {
            String commandPath = path + ".commands[" + i + "]";
            Map<String, Object> command = commands.get(i);
            warnUnknownKeys(command, commandPath, diagnostics, "execution", "command");
            String executionValue = requiredString(command, "execution", commandPath).toUpperCase(Locale.ROOT);
            ChallengeRewards.CommandExecution execution;
            try {
                execution = ChallengeRewards.CommandExecution.valueOf(executionValue);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unsupported command execution '" + executionValue + "' at " + commandPath + ".execution");
            }
            parsed.add(new ChallengeRewards.CommandSpec(execution, requiredString(command, "command", commandPath)));
        }
        return new ChallengeRewards.CommandReward(parsed);
    }

    private List<ChallengeRequirements.RankUnlockRequirement> parseRankUnlockRequirements(List<Map<?, ?>> entries, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        List<ChallengeRequirements.RankUnlockRequirement> requirements = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            return requirements;
        }
        for (int i = 0; i < entries.size(); i++) {
            String entryPath = path + "[" + i + "]";
            Map<String, Object> entry = normalizeMap(entries.get(i), entryPath);
            Object requirement = parseTaggedRequirement(entry, entryPath, diagnostics);
            if (!(requirement instanceof ChallengeRequirements.RankUnlockRequirement rankRequirement)) {
                throw new IllegalArgumentException("Requirement type at " + entryPath + " is not valid for rank unlocks");
            }
            requirements.add(rankRequirement);
        }
        return requirements;
    }

    private List<ChallengeRequirements.ChallengeUnlockRequirement> parseChallengeUnlockRequirements(List<Map<?, ?>> entries, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        List<ChallengeRequirements.ChallengeUnlockRequirement> requirements = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            return requirements;
        }
        for (int i = 0; i < entries.size(); i++) {
            String entryPath = path + "[" + i + "]";
            Map<String, Object> entry = normalizeMap(entries.get(i), entryPath);
            Object requirement = parseTaggedRequirement(entry, entryPath, diagnostics);
            if (!(requirement instanceof ChallengeRequirements.ChallengeUnlockRequirement challengeRequirement)) {
                throw new IllegalArgumentException("Requirement type at " + entryPath + " is not valid for challenge unlocks");
            }
            requirements.add(challengeRequirement);
        }
        return requirements;
    }

    private List<ChallengeRequirements.CompletionRequirement> parseCompletionRequirements(List<Map<?, ?>> entries, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        List<ChallengeRequirements.CompletionRequirement> requirements = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            return requirements;
        }
        for (int i = 0; i < entries.size(); i++) {
            String entryPath = path + "[" + i + "]";
            Map<String, Object> entry = normalizeMap(entries.get(i), entryPath);
            Object requirement = parseTaggedRequirement(entry, entryPath, diagnostics);
            if (!(requirement instanceof ChallengeRequirements.CompletionRequirement completionRequirement)) {
                throw new IllegalArgumentException("Requirement type at " + entryPath + " is not valid for completion requirements");
            }
            requirements.add(completionRequirement);
        }
        return requirements;
    }

    private Object parseTaggedRequirement(Map<String, Object> entry, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        String type = requiredString(entry, "type", path).toLowerCase(Locale.ROOT);
        return switch (type) {
            case "completed-challenges" -> {
                warnUnknownKeys(entry, path, diagnostics, "type", "challenges");
                yield new ChallengeRequirements.CompletedChallengesRequirement(
                    requiredStringList(entry, "challenges", path).stream().map(ChallengeId::of).toList()
                );
            }
            case "completed-rank" -> {
                warnUnknownKeys(entry, path, diagnostics, "type", "rank", "minimumCompletedChallenges");
                yield new ChallengeRequirements.CompletedRankRequirement(
                    RankId.of(requiredString(entry, "rank", path)),
                    requiredInt(entry, "minimumCompletedChallenges", path)
                );
            }
            case "permission" -> {
                warnUnknownKeys(entry, path, diagnostics, "type", "permission");
                yield new ChallengeRequirements.PermissionRequirement(requiredString(entry, "permission", path));
            }
            case "island-level" -> {
                warnUnknownKeys(entry, path, diagnostics, "type", "minimum");
                yield new ChallengeRequirements.IslandLevelRequirement(requiredDouble(entry, "minimum", path));
            }
            case "inventory-items" -> parseInventoryItemsRequirement(entry, path, diagnostics);
            case "island-blocks" -> parseIslandBlocksRequirement(entry, path, diagnostics);
            case "entity-presence" -> parseEntityPresenceRequirement(entry, path, diagnostics);
            default -> throw new IllegalArgumentException("Unsupported requirement type '" + type + "' at " + path);
        };
    }

    private ChallengeRequirements.InventoryItemsRequirement parseInventoryItemsRequirement(Map<String, Object> entry, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        warnUnknownKeys(entry, path, diagnostics, "type", "items");
        List<Map<String, Object>> items = requiredMapList(entry, "items", path);
        List<ChallengeRequirements.ItemRequirementSpec> parsed = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            String itemPath = path + ".items[" + i + "]";
            Map<String, Object> item = items.get(i);
            warnUnknownKeys(item, itemPath, diagnostics, "item", "amount", "progression");
            ItemStackSpec itemSpec = gameObjects.itemStack(requiredString(item, "item", itemPath));
            int amount = requiredInt(item, "amount", itemPath);
            ChallengeRequirements.ItemAmountProgression progression = parseItemProgression(item.get("progression"), itemPath + ".progression", diagnostics);
            parsed.add(new ChallengeRequirements.ItemRequirementSpec(itemSpec, amount, progression));
        }
        return new ChallengeRequirements.InventoryItemsRequirement(parsed);
    }

    private ChallengeRequirements.IslandBlocksRequirement parseIslandBlocksRequirement(Map<String, Object> entry, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        warnUnknownKeys(entry, path, diagnostics, "type", "radius", "blocks");
        int radius = requiredInt(entry, "radius", path);
        List<Map<String, Object>> blocks = requiredMapList(entry, "blocks", path);
        List<ChallengeRequirements.BlockRequirementSpec> parsed = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            String blockPath = path + ".blocks[" + i + "]";
            Map<String, Object> block = blocks.get(i);
            warnUnknownKeys(block, blockPath, diagnostics, "block", "amount");
            String blockSpec = requiredString(block, "block", blockPath);
            int amount = requiredInt(block, "amount", blockPath);
            BlockRequirement requirement = ItemStackUtil.createBlockRequirement(blockSpec + ":" + amount);
            parsed.add(new ChallengeRequirements.BlockRequirementSpec(requirement.type(), requirement.amount()));
        }
        return new ChallengeRequirements.IslandBlocksRequirement(parsed, radius);
    }

    private ChallengeRequirements.EntityPresenceRequirement parseEntityPresenceRequirement(Map<String, Object> entry, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        warnUnknownKeys(entry, path, diagnostics, "type", "radius", "entities");
        int radius = requiredInt(entry, "radius", path);
        List<Map<String, Object>> entities = requiredMapList(entry, "entities", path);
        List<ChallengeRequirements.EntityRequirementSpec> parsed = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            String entityPath = path + ".entities[" + i + "]";
            Map<String, Object> entity = entities.get(i);
            warnUnknownKeys(entity, entityPath, diagnostics, "entity", "count", "metadata");
            String entityName = requiredString(entity, "entity", entityPath);
            EntityType entityType = parseEntityType(entityName);
            if (entityType == null) {
                throw new IllegalArgumentException("Unknown entity type '" + entityName + "' at " + entityPath + ".entity");
            }
            int count = requiredInt(entity, "count", entityPath);
            Map<String, Object> metadata = optionalStringKeyedMap(entity.get("metadata"), entityPath + ".metadata");
            parsed.add(new ChallengeRequirements.EntityRequirementSpec(entityType, metadata, count));
        }
        return new ChallengeRequirements.EntityPresenceRequirement(parsed, radius);
    }

    private static @Nullable EntityType parseEntityType(String rawName) {
        String normalized = rawName.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }
        normalized = normalized.replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return EntityType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private ChallengeRequirements.ItemAmountProgression parseItemProgression(@Nullable Object rawValue, String path, List<ChallengeCatalogDiagnostic> diagnostics) {
        if (rawValue == null) {
            return ChallengeRequirements.ItemAmountProgression.none();
        }
        if (!(rawValue instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("Expected object at " + path);
        }
        Map<String, Object> map = normalizeMap(rawMap, path);
        warnUnknownKeys(map, path, diagnostics, "operator", "increment");
        String operatorValue = requiredString(map, "operator", path).toUpperCase(Locale.ROOT);
        try {
            return new ChallengeRequirements.ItemAmountProgression(
                dk.lockfuglsang.minecraft.util.ItemRequirement.Operator.valueOf(operatorValue),
                requiredDouble(map, "increment", path)
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported item progression operator '" + operatorValue + "' at " + path + ".operator");
        }
    }

    private static ConfigurationSection requiredSection(ConfigurationSection section, String key, String path) {
        ConfigurationSection result = section.getConfigurationSection(key);
        if (result == null) {
            throw new IllegalArgumentException("Missing required section '" + path + "." + key + "'");
        }
        return result;
    }

    private static String requiredString(ConfigurationSection section, String key, String path) {
        String value = section.getString(key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required string '" + path + "." + key + "'");
        }
        return value;
    }

    private static java.util.Optional<String> optionalString(ConfigurationSection section, String key) {
        String value = section.getString(key, null);
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(value);
    }

    private static String requiredString(Map<String, Object> map, String key, String path) {
        Object value = map.get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException("Missing required string '" + path + "." + key + "'");
        }
        return stringValue;
    }

    private static int requiredInt(Map<String, Object> map, String key, String path) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Missing required integer '" + path + "." + key + "'");
    }

    private static double requiredDouble(Map<String, Object> map, String key, String path) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException("Missing required number '" + path + "." + key + "'");
    }

    private static double requiredDouble(Map<String, Object> map, String key, String path, double defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException("Invalid number at '" + path + "." + key + "'");
    }

    private static List<String> requiredStringList(Map<String, Object> map, String key, String path) {
        Object value = map.get(key);
        if (!(value instanceof List<?> rawList)) {
            throw new IllegalArgumentException("Missing required list '" + path + "." + key + "'");
        }
        List<String> result = new ArrayList<>();
        for (Object entry : rawList) {
            if (!(entry instanceof String stringEntry) || stringEntry.isBlank()) {
                throw new IllegalArgumentException("Invalid string list entry in '" + path + "." + key + "'");
            }
            result.add(stringEntry);
        }
        return result;
    }

    private static List<Map<String, Object>> requiredMapList(Map<String, Object> map, String key, String path) {
        Object value = map.get(key);
        if (!(value instanceof List<?> rawList)) {
            throw new IllegalArgumentException("Missing required list '" + path + "." + key + "'");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            result.add(normalizeMap(rawList.get(i), path + "." + key + "[" + i + "]"));
        }
        return result;
    }

    private static Map<String, Object> optionalStringKeyedMap(@Nullable Object rawValue, String path) {
        if (rawValue == null) {
            return Map.of();
        }
        return normalizeMap(rawValue, path);
    }

    private static Map<String, Object> normalizeMap(@Nullable Object rawValue, String path) {
        if (!(rawValue instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("Expected object at " + path);
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("Expected string key in object at " + path);
            }
            normalized.put(key, entry.getValue());
        }
        return normalized;
    }

    private static Duration parseDuration(String rawValue, String path) {
        try {
            return ConfigDuration.parse(rawValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid duration at " + path + ": " + rawValue);
        }
    }

    private static void warnUnknownKeys(ConfigurationSection section, String path, List<ChallengeCatalogDiagnostic> diagnostics, String... allowedKeys) {
        warnUnknownKeys(new LinkedHashSet<>(section.getKeys(false)), path, diagnostics, allowedKeys);
    }

    private static void warnUnknownKeys(Map<String, Object> map, String path, List<ChallengeCatalogDiagnostic> diagnostics, String... allowedKeys) {
        warnUnknownKeys(map.keySet(), path, diagnostics, allowedKeys);
    }

    private static void warnUnknownKeys(Set<String> keys, String path, List<ChallengeCatalogDiagnostic> diagnostics, String... allowedKeys) {
        Set<String> allowed = Set.of(allowedKeys);
        for (String key : keys) {
            if (!allowed.contains(key)) {
                diagnostics.add(warn(path + "." + key, "Unknown key"));
            }
        }
    }

    private static ChallengeCatalogDiagnostic warn(String path, String message) {
        return new ChallengeCatalogDiagnostic(ChallengeCatalogDiagnostic.Severity.WARNING, path, message);
    }
}
