package us.talabrek.ultimateskyblock.imports.storage;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.model.CenterLocation;
import us.talabrek.ultimateskyblock.api.model.Island;
import us.talabrek.ultimateskyblock.api.model.IslandAccess;
import us.talabrek.ultimateskyblock.api.model.IslandAccessList;
import us.talabrek.ultimateskyblock.api.model.IslandLocation;
import us.talabrek.ultimateskyblock.api.model.IslandLocations;
import us.talabrek.ultimateskyblock.api.model.IslandLog;
import us.talabrek.ultimateskyblock.api.model.IslandLogLine;
import us.talabrek.ultimateskyblock.api.model.IslandParty;
import us.talabrek.ultimateskyblock.api.model.IslandPartyMember;
import us.talabrek.ultimateskyblock.api.model.Player;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Stream;

public class IslandImporter {
    private final uSkyBlock plugin;

    public IslandImporter(uSkyBlock plugin) {
        this.plugin = plugin;
        importFiles();
    }

    private void importFiles() {
        try (Stream<Path> files = Files.list(plugin.getDataFolder().toPath().resolve("islands"))) {
            AtomicInteger importCount = new AtomicInteger(0);

            files
                .filter(file -> !Files.isDirectory(file))
                .filter(file -> file.getFileName().toString().endsWith(".yml"))
                .forEach(islandFile -> {
                    try {
                        String islandName = islandFile.getFileName().toString().split("\\.")[0];
                        Island island = new Island(UUID.randomUUID(), islandName);

                        YamlConfiguration islandConfig = YamlConfiguration.loadConfiguration(islandFile.toFile());
                        island.setOwner(UUID.fromString(islandConfig.getString("party.leader-uuid")));

                        String[] islandLoc = islandName.split(",");
                        CenterLocation islandLocation = new CenterLocation(
                            Integer.parseInt(islandLoc[0]),
                            Integer.parseInt(islandLoc[1])
                        );
                        island.setLocation(islandLocation);

                        island.setIgnore(islandConfig.getBoolean("general.ignore", false));
                        island.setLocked(islandConfig.getBoolean("general.locked", false));
                        island.setWarpActive(islandConfig.getBoolean("general.warpActive", false));
                        island.setRegionVersion(islandConfig.getString("general.regionVersion", ""));
                        island.setSchematicName(islandConfig.getString("general.schematicName", ""));
                        island.setLevel(islandConfig.getDouble("general.level", 0D));
                        island.setScoreMultiplier(islandConfig.getDouble("general.scoreMultiply", 1D));
                        island.setScoreOffset(islandConfig.getDouble("general.scoreOffset", 0D));
                        island.setBiome(islandConfig.getString("general.biome", ""));
                        island.setLeafBreaks(islandConfig.getInt("blocks.leafBreaks", 0));
                        island.setHopperCount(islandConfig.getInt("blocks.hopperCount", 0));

                        island.setIslandAccessList(parseAccessList(island, islandConfig));
                        island.setIslandLocations(parseIslandLocations(island, islandConfig));
                        island.setIslandParty(parsePartyMembers(island, islandConfig));
                        island.setIslandLog(parseLog(island, islandConfig));

                        importOfflinePlayers(island);
                        plugin.getStorage().saveIsland(island);
                        int count = importCount.incrementAndGet();

                        if (count % 20 == 0) {
                            Thread.sleep(400);
                        }

                        if (count % 100 == 0) {
                            plugin.getLogger().info("Loaded " + count + " islands already...");
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to import island " + islandFile.getFileName().toString(), ex);
                    }
                });

            Files.move(plugin.getDataFolder().toPath().resolve("islands"), plugin.getDataFolder().toPath().resolve("islands_imported"), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Imported " + importCount.get() + " islands.");
            plugin.getLogger().info("Moved uSkyBlock/islands/ to uSkyBlock/islands_imported/.");
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to collect island files.", ex);
        }
    }

    /**
     * Make sure to import all UUID's from Bukkit into our Player DB. Should never look up at Mojang
     * because all UUID's found here have played SkyBlock before.
     */
    private void importOfflinePlayers(Island island) {
        importOfflinePlayer(island.getOwner());
        island.getIslandAccessList().getAcl().keySet().forEach(this::importOfflinePlayer);
        island.getIslandParty().getPartyMembers().keySet().forEach(this::importOfflinePlayer);
    }

    private void importOfflinePlayer(UUID uuid) {
        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
        Player player = new Player(offlinePlayer.getUniqueId(), offlinePlayer.getName(), offlinePlayer.getName());
        plugin.getStorage().savePlayer(player);
    }

    private IslandAccessList parseAccessList(Island island, YamlConfiguration islandConfig) {
        IslandAccessList accessList = new IslandAccessList(island);

        List<String> trustees = islandConfig.getStringList("trust.list");
        trustees.forEach(trustee -> {
            IslandAccess islandAccess = new IslandAccess(UUID.fromString(trustee), IslandAccess.AccessType.TRUSTED);
            accessList.addIslandAccess(islandAccess);
        });

        List<String> banned = islandConfig.getStringList("banned.list");
        banned.forEach(ban -> {
            IslandAccess islandAccess = new IslandAccess(UUID.fromString(ban), IslandAccess.AccessType.BANNED);
            accessList.addIslandAccess(islandAccess);
        });

        return accessList;
    }

    private IslandLocations parseIslandLocations(Island island, YamlConfiguration islandConfig) {
        IslandLocations islandLocations = new IslandLocations(island);

        if (islandConfig.getBoolean("general.warpActive")) {
            IslandLocation location = new IslandLocation(IslandLocation.LocationType.WARP);
            location.setWorld(Settings.general_worldName);
            location.setX(islandConfig.getDouble("general.warpLocationX"));
            location.setY(islandConfig.getDouble("general.warpLocationY"));
            location.setZ(islandConfig.getDouble("general.warpLocationZ"));
            location.setPitch(0);
            location.setYaw(0);

            islandLocations.addLocation(IslandLocation.LocationType.WARP, location);
        }

        return islandLocations;
    }

    private IslandLog parseLog(Island island, YamlConfiguration islandConfig) {
        IslandLog islandLog = new IslandLog(island);

        islandConfig.getStringList("log").forEach(logLine -> {
            String[] logParts = logLine.split(";");
            IslandLogLine islandLogLine = new IslandLogLine(
                Instant.ofEpochMilli(Long.parseLong(logParts[0])),
                logParts[1],
                Arrays.copyOfRange(logParts, 2, logParts.length));

            islandLog.log(islandLogLine);
        });

        return islandLog;
    }

    private IslandParty parsePartyMembers(Island island, YamlConfiguration islandConfig) {
        IslandParty islandParty = new IslandParty(island);

        ConfigurationSection partyConfig = islandConfig.getConfigurationSection("party");
        partyConfig.getConfigurationSection("members").getKeys(false).forEach(memberKey -> {
            ConfigurationSection memberConfig = partyConfig.getConfigurationSection("members." + memberKey);
            IslandPartyMember partyMember = new IslandPartyMember(UUID.fromString(memberKey));

            if (partyConfig.getString("leader-uuid").equals(partyMember.getUuid().toString())) {
                partyMember.setRole(IslandPartyMember.Role.LEADER);
            } else {
                partyMember.setRole(IslandPartyMember.Role.MEMBER);
            }

            partyMember.setCanChangeBiome(memberConfig.getBoolean("canChangeBiome", false));
            partyMember.setCanToggleLock(memberConfig.getBoolean("canToggleLock", false));
            partyMember.setCanChangeWarp(memberConfig.getBoolean("canChangeWarp", false));
            partyMember.setCanToggleWarp(memberConfig.getBoolean("canToggleWarp", false));
            partyMember.setCanInviteOthers(memberConfig.getBoolean("canInviteOthers", false));
            partyMember.setCanKickOthers(memberConfig.getBoolean("canKickOthers", false));
            partyMember.setCanBanOthers(memberConfig.getBoolean("canBanOthers", false));
            partyMember.setMaxAnimals(memberConfig.getInt("maxAnimals", 64));
            partyMember.setMaxMonsters(memberConfig.getInt("maxMonsters", 50));
            partyMember.setMaxVillagers(memberConfig.getInt("maxVillagers", 32));
            partyMember.setMaxGolems(memberConfig.getInt("maxGolems", 5));

            islandParty.addPartyMember(partyMember.getUuid(), partyMember);
        });

        return islandParty;
    }
}
