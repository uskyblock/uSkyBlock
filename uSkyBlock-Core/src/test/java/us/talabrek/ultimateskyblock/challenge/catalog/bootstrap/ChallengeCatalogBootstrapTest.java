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
