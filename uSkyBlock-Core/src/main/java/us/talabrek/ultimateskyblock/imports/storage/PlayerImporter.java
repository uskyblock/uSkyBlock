package us.talabrek.ultimateskyblock.imports.storage;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.model.PendingPlayerOperation;
import us.talabrek.ultimateskyblock.api.model.PendingPlayerOperation.OperationType;
import us.talabrek.ultimateskyblock.api.model.PendingPlayerOperations;
import us.talabrek.ultimateskyblock.api.model.Player;
import us.talabrek.ultimateskyblock.api.model.PlayerLocation;
import us.talabrek.ultimateskyblock.api.model.PlayerLocations;
import us.talabrek.ultimateskyblock.storage.SkyStorage;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class PlayerImporter {
    private final uSkyBlock plugin;
    private final SkyStorage storage;
    private final YamlConfiguration uuidCache;

    public PlayerImporter(uSkyBlock plugin, SkyStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        File dataFile = new File(plugin.getDataFolder(), "uuid2name.yml");
        uuidCache = loadUuidCache(dataFile);

        importFiles();

        try {
            Files.move(dataFile.toPath(), plugin.getDataFolder().toPath().resolve("uuid2name.old"));
        } catch (IOException ex) {
            plugin.getLog4JLogger().error("Failed to move uuid2name.yml.", ex);
        }
    }

    private YamlConfiguration loadUuidCache(File dataFile) {
        if (dataFile.exists()) {
            try {
                return YamlConfiguration.loadConfiguration(dataFile);
            } catch (Exception ex) {
                plugin.getLog4JLogger().warn("Failed to load uuid2name.yml.", ex);
            }
        }

        return null;
    }

    private void importFiles() {
        try (Stream<Path> files = Files.list(plugin.getDataFolder().toPath().resolve("players"))) {
            AtomicInteger importCount = new AtomicInteger(0);

            files
                .filter(file -> !Files.isDirectory(file))
                .filter(file -> file.getFileName().toString().endsWith(".yml"))
                .forEach(playerFile -> {
                    try {
                        String playerUuid = playerFile.getFileName().toString().split("\\.")[0];
                        UUID uuid = UUID.fromString(playerUuid);
                        Player player = preloadPlayer(uuid);

                        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile.toFile());

                        PendingPlayerOperations pendingOperations = new PendingPlayerOperations(player);
                        if (playerConfig.contains("pending-permissions")) {
                            playerConfig.getStringList("pending-permissions").forEach(permission -> {
                                PendingPlayerOperation operation = new PendingPlayerOperation(
                                    OperationType.PERMISSION,
                                    permission
                                );

                                pendingOperations.addPendingOperation(operation);
                            });
                        }

                        if (playerConfig.contains("pending-commands")) {
                            playerConfig.getStringList("pending-commands").forEach(command -> {
                                PendingPlayerOperation operation = new PendingPlayerOperation(
                                    OperationType.COMMAND,
                                    command
                                );

                                pendingOperations.addPendingOperation(operation);
                            });
                        }

                        player.setPendingOperations(pendingOperations);
                        player.setClearInventory(playerConfig.getBoolean("clearInventoryOnNextEntry", false));
                        player.setPlayerLocations(parsePlayerLocations(player, playerConfig));

                        storage.savePlayer(player);
                        int count = importCount.incrementAndGet();

                        if (count % 20 == 0) {
                            Thread.sleep(400);
                        }

                        if (count % 100 == 0) {
                            plugin.getLog4JLogger().info("Loaded {} players already...", count);
                        }
                    } catch (Exception ex) {
                        plugin.getLog4JLogger().error("Failed to import player {}", playerFile.getFileName().toString(), ex);
                    }
                });
            Files.move(plugin.getDataFolder().toPath().resolve("players"), plugin.getDataFolder().toPath().resolve("players_imported"), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLog4JLogger().info("Imported {} players.", importCount.get());
            plugin.getLog4JLogger().info("Moved uSkyBlock/players/ to uSkyBlock/players_imported/.");
        } catch (IOException ex) {
            plugin.getLog4JLogger().error("Failed to collect player files.", ex);
        }
    }

    private PlayerLocations parsePlayerLocations(Player player, YamlConfiguration playerConfig) {
        PlayerLocations playerLocations = new PlayerLocations(player);

        if (playerConfig.contains("player.homeX")) {
            PlayerLocation location = new PlayerLocation(PlayerLocation.LocationType.HOME);
            location.setWorld(Settings.general_worldName);
            location.setX(playerConfig.getDouble("player.homeX"));
            location.setY(playerConfig.getDouble("player.homeY"));
            location.setZ(playerConfig.getDouble("player.homeZ"));
            location.setYaw(playerConfig.getDouble("player.homeYaw"));
            location.setPitch(playerConfig.getDouble("player.homePitch"));

            playerLocations.addLocation(PlayerLocation.LocationType.HOME, location);
        }

        return playerLocations;
    }

    private Player preloadPlayer(UUID uuid) {
        if (uuidCache != null) {
            return new Player(
                uuid,
                uuidCache.getString(uuid.toString() + ".name"),
                uuidCache.getString(uuid.toString() + ".displayName")
            );
        }

        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
        return new Player(offlinePlayer.getUniqueId(), offlinePlayer.getName(), offlinePlayer.getName());
    }
}
