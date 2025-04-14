package us.talabrek.ultimateskyblock.imports.storage;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.model.Island;
import us.talabrek.ultimateskyblock.api.model.IslandAccess;
import us.talabrek.ultimateskyblock.api.model.IslandAccessList;
import us.talabrek.ultimateskyblock.api.model.IslandLimits;
import us.talabrek.ultimateskyblock.api.model.IslandLocation;
import us.talabrek.ultimateskyblock.api.model.IslandLocations;
import us.talabrek.ultimateskyblock.api.model.IslandLog;
import us.talabrek.ultimateskyblock.api.model.IslandLogLine;
import us.talabrek.ultimateskyblock.api.model.IslandParty;
import us.talabrek.ultimateskyblock.api.model.IslandPartyMember;
import us.talabrek.ultimateskyblock.api.model.Player;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.UUIDUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class IslandImporter {
    private static final Pattern OLD_LOG_PATTERN = Pattern.compile("\u00a7d\\[(?<date>[^\\]]+)\\]\u00a77 (?<msg>.*)");
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

                        // Update if necessary before importing:
                        if (islandConfig.getInt("version", 0) < 3 || islandConfig.contains("maxSize")) {
                            updateConfig(islandConfig);
                        }
                        updateConfig(islandConfig);

                        String[] islandLoc = islandName.split(",");
                        island.setIgnore(islandConfig.getBoolean("general.ignore", false));
                        island.setLocked(islandConfig.getBoolean("general.locked", false));
                        island.setWarpActive(islandConfig.getBoolean("general.warpActive", false));
                        island.setRegionVersion(islandConfig.getString("general.regionVersion", ""));
                        island.setSchematicName(islandConfig.getString("general.schematicName", Settings.island_schematicName));
                        island.setLevel(islandConfig.getDouble("general.level", 0D));
                        island.setScoreMultiplier(islandConfig.getDouble("general.scoreMultiply", 1D));
                        island.setScoreOffset(islandConfig.getDouble("general.scoreOffset", 0D));
                        island.setBiome(parseBiome(islandConfig));
                        island.setLeafBreaks(islandConfig.getInt("blocks.leafBreaks", 0));
                        island.setHopperCount(islandConfig.getInt("blocks.hopperCount", 0));

                        island.setIslandAccessList(parseAccessList(island, islandConfig));
                        island.setIslandLimits(parseIslandLimits(island,  islandConfig));
                        island.setIslandLocations(parseIslandLocations(island, islandConfig, Integer.parseInt(islandLoc[0]), Integer.parseInt(islandLoc[1])));
                        island.setIslandParty(parsePartyMembers(island, islandConfig));
                        island.setIslandLog(parseLog(island, islandConfig));

                        importOfflinePlayers(island);
                        plugin.getStorage().saveIsland(island);
                        int count = importCount.incrementAndGet();

                        if (count % 20 == 0) {
                            Thread.sleep(400);
                        }

                        if (count % 100 == 0) {
                            plugin.getLog4JLogger().info("Loaded {} islands already...", count);
                        }
                    } catch (Exception ex) {
                        plugin.getLog4JLogger().error("Failed to import island {}", islandFile.getFileName().toString(), ex);
                    }
                });

            Files.move(plugin.getDataFolder().toPath().resolve("islands"), plugin.getDataFolder().toPath().resolve("islands_imported"), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLog4JLogger().info("Imported {} islands.", importCount.get());
            plugin.getLog4JLogger().info("Moved uSkyBlock/islands/ to uSkyBlock/islands_imported/.");
        } catch (IOException ex) {
            plugin.getLog4JLogger().error("Failed to collect island files.", ex);
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

    private IslandLimits parseIslandLimits(Island island, YamlConfiguration islandConfig) {
        IslandLimits limits = new IslandLimits(island);

        limits.setBlockLimits(parseBlockLimits(island, islandConfig));
        limits.setPluginLimits(parsePluginLimits(island, islandConfig));

        return limits;
    }

    private Map<Material, Integer> parseBlockLimits(Island island, YamlConfiguration islandConfig) {
        ConcurrentMap<Material, Integer> blockLimitMap = new ConcurrentHashMap<>();

        ConfigurationSection memberSection = islandConfig.getConfigurationSection("party.members");
        if (memberSection == null) return blockLimitMap;;

        memberSection.getKeys(false).forEach(memberName -> {
            ConfigurationSection memberConfig = memberSection.getConfigurationSection(memberName);
            if (memberConfig == null) return;

            if (memberConfig.isConfigurationSection("blockLimits")) {
                ConfigurationSection blockLimits = memberConfig.getConfigurationSection("blockLimits");
                if (blockLimits == null) return;

                blockLimits.getKeys(false).forEach(material -> {
                    Material materialType = Material.matchMaterial(material);
                    if (materialType != null) {
                        blockLimitMap.computeIfAbsent(materialType, (k) -> {
                            int memberMax = blockLimits.getInt(material, 0);
                            return blockLimitMap.compute(materialType, (key, oldValue) ->
                                oldValue != null && memberMax > 0 ? Math.max(memberMax, oldValue) : null);
                        });
                    }
                });
            }
        });

        return blockLimitMap;
    }

    private Map<String, Integer> parsePluginLimits(Island island, YamlConfiguration islandConfig) {
        ConcurrentMap<String, Integer> pluginLimitMap = new ConcurrentHashMap<>();

        ConfigurationSection memberSection = islandConfig.getConfigurationSection("party.members");
        if (memberSection == null) return pluginLimitMap;

        memberSection.getKeys(false).forEach(memberName -> {
            ConfigurationSection memberConfig = memberSection.getConfigurationSection(memberName);
            if (memberConfig == null) return;

            if (memberConfig.contains("maxPartySizePermission")) {
                int maxPartySize = memberConfig.getInt("maxPartySizePermission");
                pluginLimitMap.merge("maxPartySizePermission", maxPartySize, (existingValue, newValue) ->
                    newValue > existingValue ? newValue : existingValue);
            }

            if (memberConfig.contains("maxAnimals")) {
                int maxAnimals = memberConfig.getInt("maxAnimals");
                pluginLimitMap.merge("maxAnimals", maxAnimals, (existingValue, newValue) ->
                    newValue > existingValue ? newValue : existingValue);
            }

            if (memberConfig.contains("maxMonsters")) {
                int maxMonsters = memberConfig.getInt("maxMonsters");
                pluginLimitMap.merge("maxMonsters", maxMonsters, (existingValue, newValue) ->
                    newValue > existingValue ? newValue : existingValue);
            }

            if (memberConfig.contains("maxVillagers")) {
                int maxVillagers = memberConfig.getInt("maxVillagers");
                pluginLimitMap.merge("maxVillagers", maxVillagers, (existingValue, newValue) ->
                    newValue > existingValue ? newValue : existingValue);
            }

            if (memberConfig.contains("maxGolems")) {
                int maxGolems = memberConfig.getInt("maxGolems");
                pluginLimitMap.merge("maxGolems", maxGolems, (existingValue, newValue) ->
                    newValue > existingValue ? newValue : existingValue);
            }
        });

        return pluginLimitMap;
    }

    private IslandLocations parseIslandLocations(Island island, YamlConfiguration islandConfig, int centerX, int centerZ) {
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

        IslandLocation centerLocation = new IslandLocation(IslandLocation.LocationType.CENTER_WORLD);
        centerLocation.setWorld(Settings.general_worldName);
        centerLocation.setX(centerX);
        centerLocation.setY(Settings.island_height);
        centerLocation.setZ(centerZ);
        centerLocation.setPitch(0.0D);
        centerLocation.setYaw(0.0D);
        islandLocations.addLocation(IslandLocation.LocationType.CENTER_WORLD, centerLocation);

        if (Settings.nether_enabled) {
            IslandLocation netherCenterLocation = new IslandLocation(IslandLocation.LocationType.CENTER_NETHER);
            netherCenterLocation.setWorld(Settings.general_worldName + "_nether");
            netherCenterLocation.setX(centerX);
            netherCenterLocation.setY(Settings.nether_height);
            netherCenterLocation.setZ(centerZ);
            netherCenterLocation.setPitch(0.0D);
            netherCenterLocation.setYaw(0.0D);
            islandLocations.addLocation(IslandLocation.LocationType.CENTER_NETHER, netherCenterLocation);
        }

        return islandLocations;
    }

    private IslandLog parseLog(Island island, YamlConfiguration islandConfig) {
        IslandLog islandLog = new IslandLog(island);

        islandConfig.getStringList("log").forEach(logLine -> {
            Matcher matcher = OLD_LOG_PATTERN.matcher(logLine);
            if (matcher.matches()) {
                String date = matcher.group("date");
                Instant timestamp = Instant.now();
                try {
                    Date parsedDate = DateFormat.getDateInstance(3).parse(date);
                    timestamp = parsedDate.toInstant();
                } catch (ParseException ignored) {}

                String message = matcher.group("msg");
                IslandLogLine islandLogLine = new IslandLogLine(timestamp, message, null);
                islandLog.log(islandLogLine);
            } else {
                String[] logParts = logLine.split(";");
                IslandLogLine islandLogLine = new IslandLogLine(
                    Instant.ofEpochMilli(Long.parseLong(logParts[0])),
                    logParts[1],
                    Arrays.copyOfRange(logParts, 2, logParts.length));

                islandLog.log(islandLogLine);
            }
        });

        return islandLog;
    }

    private IslandParty parsePartyMembers(Island island, YamlConfiguration islandConfig) {
        IslandParty islandParty = new IslandParty(island);

        ConfigurationSection partyConfig = islandConfig.getConfigurationSection("party");
        partyConfig.getConfigurationSection("members").getKeys(false).forEach(memberKey -> {
            ConfigurationSection memberConfig = Objects.requireNonNull(partyConfig.getConfigurationSection("members." + memberKey));
            IslandPartyMember partyMember = new IslandPartyMember(UUID.fromString(memberKey));

            if (partyConfig.getString("leader-uuid").equals(partyMember.getUuid().toString())) {
                partyMember.setRole(IslandPartyMember.Role.LEADER);
            } else {
                partyMember.setRole(IslandPartyMember.Role.MEMBER);
            }

            if (memberConfig.getBoolean("canChangeBiome", false)) partyMember.setPermission("island.canChangeBiome");
            if (memberConfig.getBoolean("canChangeWarp", false)) partyMember.setPermission("island.canChangeWarp");
            if (memberConfig.getBoolean("canToggleLock", false)) partyMember.setPermission("island.canToggleLock");
            if (memberConfig.getBoolean("canToggleWarp", false)) partyMember.setPermission("island.canToggleWarp");
            if (memberConfig.getBoolean("canInviteOthers", false)) partyMember.setPermission("island.canInviteOthers");
            if (memberConfig.getBoolean("canKickOthers", false)) partyMember.setPermission("island.canKickOthers");
            if (memberConfig.getBoolean("canBanOthers", false)) partyMember.setPermission("island.canBanOthers");

            islandParty.addPartyMember(partyMember.getUuid(), partyMember);
        });

        return islandParty;
    }

    private void updateConfig(YamlConfiguration config) {
        // Backwards compatibility.
        if (config.contains("maxSize")) {
            int oldMaxSize = config.getInt("maxSize");
            if (oldMaxSize > Settings.general_maxPartySize) {
                ConfigurationSection leaderSection = config.getConfigurationSection("party.members." +
                    UUIDUtil.asString(UUID.fromString(config.getString("party.leader-uuid"))));
                if (leaderSection != null) {
                    leaderSection.set("maxPartySizePermission", oldMaxSize);
                }
            }
            config.set("maxSize", null);
        }

        int currentVersion = config.getInt("version", 0);
        if (currentVersion < 1) {
            config.set("version", 1);
        }
    }

    private Biome parseBiome(YamlConfiguration islandConfig) {
        String biomeName = islandConfig.getString("general.biome");
        if (biomeName == null || Registry.BIOME.match(biomeName) == null) {
            return Settings.general_defaultBiome;
        }

        return Registry.BIOME.match(biomeName);
    }
}
