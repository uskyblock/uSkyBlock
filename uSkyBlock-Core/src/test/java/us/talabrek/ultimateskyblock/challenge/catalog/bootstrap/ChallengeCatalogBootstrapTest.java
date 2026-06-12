package us.talabrek.ultimateskyblock.challenge.catalog.bootstrap;

import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletionLogic;
import us.talabrek.ultimateskyblock.config.PluginConfig;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.migration.PluginConfigMigrator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeCatalogBootstrapTest {
    @TempDir
    Path tempDir;

    @Test
    void rewritesLegacyChallengesToTheNewCatalogFormatAndMigratesSettings() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        try (var reader = Objects.requireNonNull(
            getClass().getResourceAsStream("/us/talabrek/ultimateskyblock/imports/old-default-challenges.yml"))) {
            Files.copy(reader, tempDir.resolve("challenges.yml"));
        }

        PluginConfig pluginConfig = new PluginConfig(new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger())));
        pluginConfig.reload();

        new ChallengeCatalogBootstrap(tempDir, Logger.getAnonymousLogger(), pluginConfig).bootstrap();

        YamlConfiguration challenges = YamlConfiguration.loadConfiguration(tempDir.resolve("challenges.yml").toFile());
        assertEquals(1, challenges.getInt("schemaVersion"));
        assertTrue(challenges.contains("ranks.Tier1.display.name"));
        assertEquals("<gray>Novice", challenges.getString("ranks.Tier1.display.name"));
        assertEquals("cyan_terracotta", challenges.getString("ranks.Tier1.lockedDisplayItem"));
        assertEquals("cobblestone", challenges.getString("ranks.Tier1.challenges.cobblestonegenerator.display.item"));
        assertEquals("gray_stained_glass_pane", challenges.getString("ranks.Tier1.challenges.cobblestonegenerator.lockedDisplayItem"));
        assertFalse(challenges.contains("version"));
        assertFalse(challenges.contains("broadcastText"));
        assertFalse(challenges.contains("ranks.Tier1.challenges.cobblestonegenerator.reward.text"));

        assertTrue(pluginConfig.getYamlConfig().getBoolean("options.challenges.enabled"));
        assertTrue(pluginConfig.getYamlConfig().getBoolean("options.challenges.broadcast.enabled"));
        assertEquals("&6", pluginConfig.getYamlConfig().getString("options.challenges.broadcast.prefix"));

        try (var files = Files.list(tempDir.resolve("backup").resolve("challenges"))) {
            assertTrue(files.findAny().isPresent());
        }

        assertTrue(challenges.contains("ranks.Tier3.challenges.woolcollector.rewards.first"));
        // Default sharing mode: no legacy-player-sharing flag is written.
        assertFalse(pluginConfig.getYamlConfig().contains(ChallengeCompletionLogic.LEGACY_PLAYER_SHARING_CONFIG_KEY));
    }

    @Test
    void importsLegacyProgressionOperators() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        try (var reader = Objects.requireNonNull(
            getClass().getResourceAsStream("/us/talabrek/ultimateskyblock/imports/old-default-challenges.yml"))) {
            Files.copy(reader, tempDir.resolve("challenges.yml"));
        }
        Path challengesPath = tempDir.resolve("challenges.yml");
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(challengesPath.toFile());
        // '/' is the 3.x divide symbol; '^' was a multiply alias in the pre-1.20.6 plugin.
        legacy.set("ranks.Tier1.challenges.cobblestonegenerator.requiredItems",
            java.util.List.of("COBBLESTONE:64;/2", "DIRT:16;^2"));
        legacy.save(challengesPath.toFile());

        PluginConfig pluginConfig = new PluginConfig(new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger())));
        pluginConfig.reload();

        new ChallengeCatalogBootstrap(tempDir, Logger.getAnonymousLogger(), pluginConfig).bootstrap();

        YamlConfiguration challenges = YamlConfiguration.loadConfiguration(challengesPath.toFile());
        var complete = challenges.getMapList("ranks.Tier1.challenges.cobblestonegenerator.complete");
        @SuppressWarnings("unchecked")
        var items = (java.util.List<java.util.Map<String, Object>>) complete.getFirst().get("items");
        @SuppressWarnings("unchecked")
        var divideProgression = (java.util.Map<String, Object>) items.get(0).get("progression");
        @SuppressWarnings("unchecked")
        var multiplyProgression = (java.util.Map<String, Object>) items.get(1).get("progression");
        assertEquals("divide", divideProgression.get("operator"));
        assertEquals("multiply", multiplyProgression.get("operator"));
    }

    @Test
    void importsMessyCustomFilesBestEffort() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        try (var reader = Objects.requireNonNull(
            getClass().getResourceAsStream("/us/talabrek/ultimateskyblock/imports/old-default-challenges.yml"))) {
            Files.copy(reader, tempDir.resolve("challenges.yml"));
        }
        Path challengesPath = tempDir.resolve("challenges.yml");
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(challengesPath.toFile());
        // A challenge with a broken reward amount and a rank without challenges must not
        // abort the import of everything else.
        legacy.set("ranks.Tier1.challenges.brokenchallenge.name", "Broken");
        legacy.set("ranks.Tier1.challenges.brokenchallenge.type", "onPlayer");
        legacy.set("ranks.Tier1.challenges.brokenchallenge.requiredItems", java.util.List.of("COBBLESTONE:notanumber"));
        legacy.set("ranks.Tier1.challenges.brokenchallenge.reward.items", java.util.List.of("LEATHER:alsobroken"));
        legacy.set("ranks.ZBrokenRank.name", "No challenges here");
        legacy.save(challengesPath.toFile());

        PluginConfig pluginConfig = new PluginConfig(new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger())));
        pluginConfig.reload();

        new ChallengeCatalogBootstrap(tempDir, Logger.getAnonymousLogger(), pluginConfig).bootstrap();

        YamlConfiguration challenges = YamlConfiguration.loadConfiguration(challengesPath.toFile());
        assertEquals(1, challenges.getInt("schemaVersion"));
        assertTrue(challenges.contains("ranks.Tier1.challenges.cobblestonegenerator"));
        assertFalse(challenges.contains("ranks.Tier1.challenges.brokenchallenge"));
        assertFalse(challenges.contains("ranks.ZBrokenRank"));
    }

    @Test
    void translatesBiomePermissionRewardsToBiomeRewards() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        try (var reader = Objects.requireNonNull(
            getClass().getResourceAsStream("/us/talabrek/ultimateskyblock/imports/old-default-challenges.yml"))) {
            Files.copy(reader, tempDir.resolve("challenges.yml"));
        }
        Path challengesPath = tempDir.resolve("challenges.yml");
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(challengesPath.toFile());
        legacy.set("ranks.Tier1.challenges.cobblestonegenerator.reward.permission", "usb.biome.Jungle usb.extra.perk");
        legacy.save(challengesPath.toFile());

        PluginConfig pluginConfig = new PluginConfig(new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger())));
        pluginConfig.reload();

        new ChallengeCatalogBootstrap(tempDir, Logger.getAnonymousLogger(), pluginConfig).bootstrap();

        YamlConfiguration challenges = YamlConfiguration.loadConfiguration(challengesPath.toFile());
        var rewards = challenges.getMapList("ranks.Tier1.challenges.cobblestonegenerator.rewards.first");
        var biomeReward = rewards.stream().filter(reward -> "biome".equals(reward.get("type"))).findFirst().orElseThrow();
        var permissionReward = rewards.stream().filter(reward -> "permission".equals(reward.get("type"))).findFirst().orElseThrow();
        assertEquals(java.util.List.of("jungle"), biomeReward.get("biomes"));
        assertEquals(java.util.List.of("usb.extra.perk"), permissionReward.get("permissions"));
    }

    @Test
    void leavesWhollyUnreadableFilesUntouched() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        // No ranks section at all: nothing can be imported.
        Files.writeString(tempDir.resolve("challenges.yml"), "allowChallenges: true\n");

        PluginConfig pluginConfig = new PluginConfig(new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger())));
        pluginConfig.reload();

        new ChallengeCatalogBootstrap(tempDir, Logger.getAnonymousLogger(), pluginConfig).bootstrap();

        // The file is untouched (no schemaVersion written) so nothing is lost.
        YamlConfiguration challenges = YamlConfiguration.loadConfiguration(tempDir.resolve("challenges.yml").toFile());
        assertFalse(challenges.contains("schemaVersion"));
        assertTrue(challenges.getBoolean("allowChallenges"));
    }

    @Test
    void preservesLegacyPlayerSharingFlagForProgressMigration() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        try (var reader = Objects.requireNonNull(
            getClass().getResourceAsStream("/us/talabrek/ultimateskyblock/imports/old-default-challenges.yml"))) {
            Files.copy(reader, tempDir.resolve("challenges.yml"));
        }
        Path challengesPath = tempDir.resolve("challenges.yml");
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(challengesPath.toFile());
        legacy.set("challengeSharing", "player");
        legacy.save(challengesPath.toFile());

        PluginConfig pluginConfig = new PluginConfig(new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger())));
        pluginConfig.reload();

        new ChallengeCatalogBootstrap(tempDir, Logger.getAnonymousLogger(), pluginConfig).bootstrap();

        // The import destroys the legacy challenges.yml that carried challengeSharing; the flag
        // must survive in config.yml for the one-shot SQLite progress migration to consume.
        assertTrue(pluginConfig.getYamlConfig().getBoolean(ChallengeCompletionLogic.LEGACY_PLAYER_SHARING_CONFIG_KEY));
    }
}
