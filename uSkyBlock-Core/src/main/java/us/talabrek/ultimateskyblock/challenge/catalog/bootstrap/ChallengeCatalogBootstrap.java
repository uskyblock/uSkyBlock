package us.talabrek.ultimateskyblock.challenge.catalog.bootstrap;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.PluginConfig;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ChallengeCatalogBootstrap {
    private final Path pluginDataDir;
    private final Logger logger;
    private final PluginConfig pluginConfig;

    public ChallengeCatalogBootstrap(@NotNull Path pluginDataDir, @NotNull Logger logger, @NotNull PluginConfig pluginConfig) {
        this.pluginDataDir = pluginDataDir;
        this.logger = logger;
        this.pluginConfig = pluginConfig;
    }

    public void bootstrap() {
        LegacyChallengeCatalogImporter importer = new LegacyChallengeCatalogImporter(pluginDataDir, logger, pluginConfig);
        importer.ensureChallengeFileExists();

        Path challengesPath = pluginDataDir.resolve(ChallengeCatalogLoader.CHALLENGES_FILE_NAME);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(challengesPath.toFile());
        if (importer.needsImport(config)) {
            try {
                importer.importLegacyFile(challengesPath);
            } catch (RuntimeException e) {
                // Best effort: leave the legacy file untouched so nothing is lost; the catalog
                // loader runs with an empty catalog until the file is fixed or removed.
                logger.log(Level.SEVERE, "Unable to import the legacy challenges.yml; the file was left untouched"
                    + " and challenges are disabled until it is fixed or removed.", e);
            }
        }
    }
}
