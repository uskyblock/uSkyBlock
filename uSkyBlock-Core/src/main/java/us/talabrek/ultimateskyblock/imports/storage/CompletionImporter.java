package us.talabrek.ultimateskyblock.imports.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import us.talabrek.ultimateskyblock.api.model.ChallengeCompletion;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Stream;

public class CompletionImporter {
    private final uSkyBlock plugin;

    public CompletionImporter(uSkyBlock plugin) {
        this.plugin = plugin;
        importFiles();
    }

    private void importFiles() {
        try (Stream<Path> files = Files.list(plugin.getDataFolder().toPath().resolve("completion"))) {
            AtomicInteger importCount = new AtomicInteger(0);

            YamlConfiguration challengeConfiguration = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "challenges.yml"));
            String challengeSharing = challengeConfiguration.getString("challengeSharing", "island");

            files
                .filter(file -> !Files.isDirectory(file))
                .filter(file -> file.getFileName().toString().endsWith(".yml"))
                .forEach(completionFile -> {
                    try {
                        YamlConfiguration completionConfig = YamlConfiguration.loadConfiguration(completionFile.toFile());
                        int count = 0;

                        if (challengeSharing.equalsIgnoreCase("island")) {
                            parseIslandCompletion(completionConfig, completionFile.getFileName().toString())
                                .forEach(completion -> plugin.getStorage().saveChallengeCompletion(completion));
                            count = importCount.incrementAndGet();
                        } else if (challengeSharing.equalsIgnoreCase("player")) {
                            parsePlayerCompletion(completionConfig, completionFile.getFileName().toString())
                                .forEach(completion -> plugin.getStorage().saveChallengeCompletion(completion));
                            count = importCount.incrementAndGet();
                        }

                        if (count % 20 == 0) {
                            Thread.sleep(400);
                        }

                        if (count % 100 == 0) {
                            plugin.getLogger().info("Loaded " + count + " completions already...");
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to import completion " + completionFile.getFileName().toString(), ex);
                    }
                });

            Files.move(plugin.getDataFolder().toPath().resolve("completion"), plugin.getDataFolder().toPath().resolve("completion_imported"), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Imported " + importCount.get() + " completion files.");
            plugin.getLogger().info("Moved uSkyBlock/completion/ to uSkyBlock/completion_imported/.");
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to collect completion files.", ex);
        }
    }

    private Set<ChallengeCompletion> parseIslandCompletion(YamlConfiguration completion, String fileName) {
        String islandName = fileName.split("\\.")[0];
        UUID islandUuid = plugin.getStorage().getIslandByName(islandName).join();

        if (islandUuid == null) {
            plugin.getLogger().warning("Could not find island with name " + islandName);
        }

        Set<ChallengeCompletion> completions = new HashSet<>();

        completion.getKeys(false).forEach(challengeName -> {
            ConfigurationSection challengeSection = completion.getConfigurationSection(challengeName);

            ChallengeCompletion challengeCompletion = new ChallengeCompletion(
                islandUuid,
                ChallengeCompletion.CompletionSharing.ISLAND,
                challengeName);

            if (challengeSection.getLong("firstCompleted") == 0L && challengeSection.getInt("timesCompleted") == 0) {
                return;
            }

            challengeCompletion.setFirstCompleted(Instant.ofEpochMilli(challengeSection.getLong("firstCompleted")));
            challengeCompletion.setTimesCompleted(challengeSection.getInt("timesCompleted", 0));
            challengeCompletion.setTimesCompletedSinceTimer(challengeSection.getInt("timesCompletedSinceTimer", 0));

            completions.add(challengeCompletion);
        });

        return completions;
    }

    private Set<ChallengeCompletion> parsePlayerCompletion(YamlConfiguration completion, String fileName) {
        String playerName = fileName.split("\\.")[0];

        try {
            UUID playerUuid = UUID.fromString(playerName);

            Set<ChallengeCompletion> completions = new HashSet<>();

            completion.getKeys(false).forEach(challengeName -> {
                ConfigurationSection challengeSection = completion.getConfigurationSection(challengeName);

                ChallengeCompletion challengeCompletion = new ChallengeCompletion(
                    playerUuid,
                    ChallengeCompletion.CompletionSharing.PLAYER,
                    challengeName);

                challengeCompletion.setFirstCompleted(Instant.ofEpochMilli(challengeSection.getLong("firstCompleted")));
                challengeCompletion.setTimesCompleted(challengeSection.getInt("timesCompleted", 0));
                challengeCompletion.setTimesCompletedSinceTimer(challengeSection.getInt("timesCompletedSinceTimer", 0));

                completions.add(challengeCompletion);
            });

            return completions;
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid player UUID" + playerName);
            return Set.of();
        }
    }
}
