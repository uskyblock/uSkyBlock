package us.talabrek.ultimateskyblock.challenge.catalog.bootstrap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalogDiagnostic;
import us.talabrek.ultimateskyblock.challenge.catalog.yaml.ChallengeCatalogParseResult;
import us.talabrek.ultimateskyblock.challenge.catalog.yaml.ChallengeCatalogYamlParser;

import java.nio.file.Path;
import java.util.logging.Logger;

@Singleton
public final class ChallengeCatalogLoader {
    public static final String CHALLENGES_FILE_NAME = "challenges.yml";

    private final Path challengesPath;
    private final Logger logger;
    private final ChallengeCatalogYamlParser parser;

    @Inject
    public ChallengeCatalogLoader(
        @NotNull @PluginDataDir Path pluginDataDir,
        @NotNull @PluginLog Logger logger,
        @NotNull ChallengeCatalogYamlParser parser
    ) {
        this.challengesPath = pluginDataDir.resolve(CHALLENGES_FILE_NAME);
        this.logger = logger;
        this.parser = parser;
    }

    public @NotNull ChallengeCatalog load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(challengesPath.toFile());
        ChallengeCatalogParseResult result = parser.parse(config);
        for (ChallengeCatalogDiagnostic diagnostic : result.diagnostics()) {
            if (diagnostic.severity() == ChallengeCatalogDiagnostic.Severity.WARNING) {
                logger.warning("Challenge catalog warning at " + diagnostic.path() + ": " + diagnostic.message());
            } else {
                logger.severe("Challenge catalog error at " + diagnostic.path() + ": " + diagnostic.message());
            }
        }
        return result.catalog();
    }
}
