package us.talabrek.ultimateskyblock.imports.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import us.talabrek.ultimateskyblock.api.model.ChallengeCompletion;
import us.talabrek.ultimateskyblock.api.model.ChallengeCompletionSet;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
                            ChallengeCompletionSet completionSet = parseIslandCompletion(completionConfig, completionFile.getFileName().toString());
                            plugin.getStorage().saveChallengeCompletion(completionSet);

                            count = importCount.incrementAndGet();
                        } else if (challengeSharing.equalsIgnoreCase("player")) {
                            ChallengeCompletionSet completionSet = parsePlayerCompletion(completionConfig, completionFile.getFileName().toString());
                            if (completionSet != null) plugin.getStorage().saveChallengeCompletion(completionSet);

                            count = importCount.incrementAndGet();
                        }

                        if (count % 20 == 0) {
                            Thread.sleep(400);
                        }

                        if (count % 100 == 0) {
                            plugin.getLog4JLogger().info("Loaded {} completions already...", count);
                        }
                    } catch (Exception ex) {
                        plugin.getLog4JLogger().error("Failed to import completion {}", completionFile.getFileName().toString(), ex);
                    }
                });

            Files.move(plugin.getDataFolder().toPath().resolve("completion"), plugin.getDataFolder().toPath().resolve("completion_imported"), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLog4JLogger().info("Imported {} completion files.", importCount.get());
            plugin.getLog4JLogger().info("Moved uSkyBlock/completion/ to uSkyBlock/completion_imported/.");
        } catch (IOException ex) {
            plugin.getLog4JLogger().error("Failed to collect completion files.", ex);
        }
    }

    private ChallengeCompletionSet parseIslandCompletion(YamlConfiguration completion, String fileName) {
        String islandName = fileName.split("\\.")[0];
        UUID islandUuid = plugin.getStorage().getIslandByName(islandName).join();

        if (islandUuid == null) {
            plugin.getLog4JLogger().warn("Could not find island with name {}", islandName);
        }

        ChallengeCompletionSet completionSet = new ChallengeCompletionSet(islandUuid, ChallengeCompletionSet.CompletionSharing.ISLAND);
        completion.getKeys(false).forEach(challengeName -> {
            ConfigurationSection challengeSection = Objects.requireNonNull(completion.getConfigurationSection(challengeName));

            ChallengeCompletion challengeCompletion = new ChallengeCompletion(islandUuid, challengeName);

            if (challengeSection.getLong("firstCompleted") == 0L && challengeSection.getInt("timesCompleted") == 0) {
                return;
            }

            challengeCompletion.setCooldownUntil(Instant.ofEpochMilli(challengeSection.getLong("firstCompleted", 0L)));
            challengeCompletion.setTimesCompleted(challengeSection.getInt("timesCompleted", 0));
            challengeCompletion.setTimesCompletedInCooldown(challengeSection.getInt("timesCompletedSinceTimer", 0));

            completionSet.setCompletion(challengeCompletion.getChallenge(), challengeCompletion);
        });

        return completionSet;
    }

    private ChallengeCompletionSet parsePlayerCompletion(YamlConfiguration completion, String fileName) {
        String playerName = fileName.split("\\.")[0];

        try {
            UUID playerUuid = UUID.fromString(playerName);

            ChallengeCompletionSet completionSet = new ChallengeCompletionSet(playerUuid, ChallengeCompletionSet.CompletionSharing.PLAYER);
            completion.getKeys(false).forEach(challengeName -> {
                ConfigurationSection challengeSection = Objects.requireNonNull(completion.getConfigurationSection(challengeName));

                ChallengeCompletion challengeCompletion = new ChallengeCompletion(playerUuid, challengeName);

                if (challengeSection.getLong("firstCompleted") == 0L && challengeSection.getInt("timesCompleted") == 0) {
                    return;
                }

                challengeCompletion.setCooldownUntil(Instant.ofEpochMilli(challengeSection.getLong("firstCompleted", 0L)));
                challengeCompletion.setTimesCompleted(challengeSection.getInt("timesCompleted", 0));
                challengeCompletion.setTimesCompletedInCooldown(challengeSection.getInt("timesCompletedSinceTimer", 0));

                completionSet.setCompletion(challengeCompletion.getChallenge(), challengeCompletion);
            });

            return completionSet;
        } catch (IllegalArgumentException ex) {
            plugin.getLog4JLogger().warn("Invalid player UUID {}", playerName);
            return null;
        }
    }
}
