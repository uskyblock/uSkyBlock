package us.talabrek.ultimateskyblock.imports;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;

import static us.talabrek.ultimateskyblock.util.FileUtil.generateTimestamp;

public class BlockRequirementConverter {

    private static final int NEW_CHALLENGES_VERSION = 108;
    private final Logger logger;

    public BlockRequirementConverter(Logger logger) {
        this.logger = logger;
    }

    public void checkAndDoImport(File directory) {
        var challengesFile = new File(directory, "challenges.yml");
        if (challengesFile.exists() && YamlConfiguration.loadConfiguration(challengesFile).getInt("version") < NEW_CHALLENGES_VERSION) {
            importFile(challengesFile);
        }
    }

    public void importFile(File file) {
        Path configFile = file.toPath();
        try {
            Files.copy(configFile, configFile.getParent().resolve(configFile.getFileName() + "_" + generateTimestamp() + ".old"));

            FileConfiguration config = new YamlConfiguration();
            config.load(file);

            if (file.getName().equals("challenges.yml")) {
                convertChallenges(config);
            }

            config.save(file);
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while attempting to convert file " + file, e);
        }
    }

    private void convertChallenges(FileConfiguration config) throws Exception {
        var oldVersion = config.getInt("version");
        if (oldVersion >= NEW_CHALLENGES_VERSION) {
            logger.warning("Expecting challanges.yml version " + (NEW_CHALLENGES_VERSION - 1) + ", but found " + oldVersion + " instead. Skipping conversion.");
            return;
        }
        logger.info("Converting challenges.yml to new block requirement format.");

        convertBlockRequirements(config);
    }

    private void convertBlockRequirements(FileConfiguration config) {
        var ranks = config.getConfigurationSection("ranks");
        if (ranks != null) {
            for (var rank : ranks.getKeys(false)) {
                var challenges = ranks.getConfigurationSection(rank + ".challenges");
                if (challenges != null) {
                    for (var challenge : challenges.getKeys(false)) {
                        var challengeSection = challenges.getConfigurationSection(challenge);
                        if (challengeSection != null
                            && challengeSection.getString("type", "").equals("onIsland")
                            && challengeSection.isSet("requiredItems")
                        ) {
                            var requiredItems = challengeSection.getStringList("requiredItems");
                            var requiredBlocks = new ArrayList<>(requiredItems);
                            challengeSection.set("requiredBlocks", requiredBlocks);
                            challengeSection.set("requiredItems", null);
                        }
                    }
                }
            }
        }

        // Add the new header with explanations and usage instructions
        var defaultConfig = new YamlConfiguration();
        try (var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream("challenges.yml")), StandardCharsets.UTF_8))) {
            defaultConfig.load(reader);
        } catch (Exception e) {
            logger.warning("Failed to load default challenges.yml file - unable to update the config header.");
        }
        config.options().setHeader(defaultConfig.options().getHeader());

        config.set("version", NEW_CHALLENGES_VERSION);
    }
}
