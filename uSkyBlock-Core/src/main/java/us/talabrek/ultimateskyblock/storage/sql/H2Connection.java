package us.talabrek.ultimateskyblock.storage.sql;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.model.ChallengeCompletion;
import us.talabrek.ultimateskyblock.api.model.ChallengeCompletionSet;
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
import us.talabrek.ultimateskyblock.api.model.PendingPlayerOperation;
import us.talabrek.ultimateskyblock.api.model.PendingPlayerOperations;
import us.talabrek.ultimateskyblock.api.model.Player;
import us.talabrek.ultimateskyblock.api.model.PlayerLocation;
import us.talabrek.ultimateskyblock.api.model.PlayerLocations;
import us.talabrek.ultimateskyblock.api.model.PlayerPermission;
import us.talabrek.ultimateskyblock.api.model.PlayerPermissions;
import us.talabrek.ultimateskyblock.api.IslandLevel;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

public class H2Connection extends SqlStorage {
    private Connection connection;
    private final Path databaseFile;

    public H2Connection(uSkyBlock plugin, Path databaseFile) {
        super(plugin);
        this.databaseFile = databaseFile;
    }

    @Override
    public void initialize() throws SQLException {
        DriverManager.registerDriver(new org.h2.Driver());
        this.connection = getConnection();

        migrate();
    }

    @Override
    public void close() throws SQLException {
        this.connection.close();
    }

    protected Connection createConnection() throws SQLException {
        return DriverManager.getConnection(getUrl());
    }

    protected Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            plugin.getLog4JLogger().info("Connecting to {}", getUrl());
            connection = createConnection();
        }

        return connection;
    }

    protected void migrate() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(getUrl());

        Flyway flyway = Flyway.configure(plugin.getClass().getClassLoader())
            .dataSource(ds)
            .locations("classpath:db/h2")
            .table("usb_flyway_history")
            .load();

        flyway.migrate();
    }

    private String getUrl() {
        return "jdbc:h2:" + databaseFile.toAbsolutePath() + ";MODE=MariaDB;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=30000";
    }

    @Override
    public ChallengeCompletionSet getChallengeCompletion(UUID uuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, sharing_type, challenge, cooldown_until, times_completed, times_completed_in_cooldown FROM usb_challenge_completion WHERE uuid = ?;")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                ChallengeCompletionSet set = null;
                while (rs.next()) {
                    UUID fetchedUuid = UUID.fromString(rs.getString("uuid"));

                    if (set == null) {
                        ChallengeCompletionSet.CompletionSharing sharingType = ChallengeCompletionSet.CompletionSharing.valueOf(rs.getString("sharing_type"));
                        set = new ChallengeCompletionSet(fetchedUuid, sharingType);
                    }

                    ChallengeCompletion completion = new ChallengeCompletion(
                        fetchedUuid,
                        rs.getString("challenge"),
                        rs.getTimestamp("cooldown_until").toInstant(),
                        rs.getInt("times_completed"),
                        rs.getInt("times_completed_in_cooldown"));

                    set.setCompletion(completion.getChallenge(), completion);
                }

                if (set != null) return set;
            }
        }

        return null;
    }

    @Override
    public void saveChallengeCompletion(ChallengeCompletionSet completionSet) throws SQLException {
        if (completionSet.isDirty()) {
            deleteChallengeCompletion(completionSet.getUuid());

            Connection c = getConnection();
            for (ChallengeCompletion completion : completionSet.getCompletionMap().values()) {
                try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_challenge_completion (uuid, sharing_type, challenge, cooldown_until, times_completed, times_completed_in_cooldown) KEY (uuid, sharing_type, challenge) VALUES (?, ?, ?, ?, ?, ?);")) {
                    ps.setString(1, completionSet.getUuid().toString());
                    ps.setString(2, completionSet.getSharingType().toString());
                    ps.setString(3, completion.getChallenge());
                    ps.setTimestamp(4, Timestamp.from(completion.getCooldownUntil()));
                    ps.setInt(5, completion.getTimesCompleted());
                    ps.setInt(6, completion.getTimesCompletedInCooldown());
                    ps.execute();
                }
            }
        }
    }

    @Override
    public int deleteChallengeCompletion(UUID uuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_challenge_completion WHERE uuid = ?;")) {
            ps.setString(1, uuid.toString());
            return ps.executeUpdate();
        }
    }

    @Override
    public @Nullable UUID getIslandByName(String name) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid FROM usb_islands WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString(1));
                }
            }
        }

        return null;
    }

    @Override
    public int getIslandCount() throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(uuid) FROM usb_islands;")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return -1;
    }

    @Override
    public @Nullable Island getIsland(UUID uuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, `name`, owner, ignore, locked, warpActive, regionVersion, schematicName, `level`, scoreMultiplier, scoreOffset, biome, leaf_breaks, hopper_count FROM usb_islands WHERE uuid = ?;")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Island island = new Island(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                    island.setOwner(UUID.fromString(rs.getString("owner")));
                    island.setIgnore(rs.getBoolean("ignore"));
                    island.setLocked(rs.getBoolean("locked"));
                    island.setWarpActive(rs.getBoolean("warpActive"));
                    island.setRegionVersion(rs.getString("regionVersion"));
                    island.setSchematicName(rs.getString("schematicName"));

                    island.setLevel(rs.getDouble("level"));
                    island.setScoreMultiplier(rs.getDouble("scoreMultiplier"));
                    island.setScoreOffset(rs.getDouble("scoreOffset"));

                    island.setBiome(Registry.BIOME.match((rs.getString("biome"))));
                    island.setLeafBreaks(rs.getInt("leaf_breaks"));
                    island.setHopperCount(rs.getInt("hopper_count"));

                    island.setIslandAccessList(getIslandAccessList(island));
                    island.setIslandLimits(getIslandLimits(island));
                    island.setIslandLocations(getIslandLocations(island));
                    island.setIslandLog(getIslandLog(island));
                    island.setIslandParty(getIslandParty(island));

                    island.setDirty(false);
                    return island;
                }
            }
        }

        return null;
    }

    @Override
    public @NotNull Set<UUID> getIslands() throws SQLException {
        ConcurrentSkipListSet<UUID> islands = new ConcurrentSkipListSet<>();

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid FROM usb_islands;")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    islands.add(UUID.fromString(rs.getString("uuid")));
                }
            }
        }

        return islands;
    }

    private IslandAccessList getIslandAccessList(Island island) throws SQLException {
        IslandAccessList acl = new IslandAccessList(island);

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT player_uuid, island_uuid, access_type FROM usb_island_access WHERE island_uuid = ?")) {
            ps.setString(1, island.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IslandAccess islandAccess = new IslandAccess(
                        UUID.fromString(rs.getString("player_uuid")),
                        IslandAccess.AccessType.valueOf(rs.getString("access_type")));

                    islandAccess.setDirty(false);
                    acl.addIslandAccess(islandAccess);
                }
            }
        }

        acl.setDirty(false);
        return acl;
    }

    private IslandLimits getIslandLimits(Island island) throws SQLException {
        IslandLimits limits = new IslandLimits(island);

        limits.setBlockLimits(getIslandBlockLimits(island));
        limits.setPluginLimits(getIslandPluginLimits(island));

        return limits;
    }

    private Map<Material, Integer> getIslandBlockLimits(Island island) throws SQLException {
        Map<Material, Integer> blockLimits = new HashMap<>();

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT entity, `limit` FROM usb_island_limits WHERE island_uuid = ? AND entity_type = ?;")) {
            ps.setString(1, island.getUuid().toString());
            ps.setString(2, "BLOCK");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Material material = Material.matchMaterial(rs.getString("entity"));
                    int limit = rs.getInt("limit");

                    if (material == null) {
                        plugin.getLog4JLogger().warn("Unknown block limit {} for island {}.", rs.getString("entity"), island.getName());
                        continue;
                    }
                    blockLimits.put(material, limit);
                }
            }
        }

        return blockLimits;
    }

    private Map<String, Integer> getIslandPluginLimits(Island island) throws SQLException {
        Map<String, Integer> pluginLimits = new HashMap<>();

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT entity, `limit` FROM usb_island_limits WHERE island_uuid = ? AND entity_type = ?;")) {
            ps.setString(1, island.getUuid().toString());
            ps.setString(2, "PLUGIN");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String entity = rs.getString("entity");
                    int limit = rs.getInt("limit");

                    pluginLimits.put(entity, limit);
                }
            }
        }

        return pluginLimits;
    }

    private IslandLocations getIslandLocations(Island island) throws SQLException {
        IslandLocations locations = new IslandLocations(island);

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT island_uuid, location_type, location_world, location_x, location_y, location_z, location_pitch, location_yaw FROM usb_island_locations WHERE island_uuid = ?")) {
            ps.setString(1, island.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IslandLocation islandLocation = new IslandLocation(
                        IslandLocation.LocationType.valueOf(rs.getString("location_type")),
                        rs.getString("location_world"),
                        rs.getDouble("location_x"),
                        rs.getDouble("location_y"),
                        rs.getDouble("location_z"),
                        rs.getDouble("location_pitch"),
                        rs.getDouble("location_yaw")
                        );

                    locations.addLocation(islandLocation.getLocationType(), islandLocation);
                }
            }
        }

        locations.setDirty(false);
        return locations;
    }

    private IslandLog getIslandLog(Island island) throws SQLException {
        IslandLog islandLog = new IslandLog(island);

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT log_uuid, island_uuid, `timestamp`, log_line, variables FROM usb_island_log WHERE island_uuid = ?;")) {
            ps.setString(1, island.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IslandLogLine logLine = new IslandLogLine(
                        UUID.fromString(rs.getString("log_uuid")),
                        rs.getTimestamp("timestamp").toInstant(),
                        rs.getString("log_line"),
                        rs.getString("variables").split(";"));

                    islandLog.log(logLine);
                }
            }
        }

        islandLog.setDirty(false);
        return islandLog;
    }

    private IslandParty getIslandParty(Island island) throws SQLException {
        IslandParty islandParty = new IslandParty(island);

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT m.player_uuid, m.island_uuid, m.`role`, GROUP_CONCAT(p.node SEPARATOR ',') AS permissions FROM usb_island_members m LEFT JOIN usb_island_member_permissions p ON m.player_uuid = p.player_uuid WHERE m.island_uuid = ? GROUP BY m.player_uuid, m.island_uuid, m.role;")) {
            ps.setString(1, island.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Set<String> permissions = Set.of();
                    if (rs.getString("permissions") != null) permissions = Set.of(rs.getString("permissions").split(","));

                    IslandPartyMember islandPartyMember = new IslandPartyMember(
                        UUID.fromString(rs.getString("player_uuid")),
                        IslandPartyMember.Role.valueOf(rs.getString("role")),
                        permissions);

                    islandParty.addPartyMember(islandPartyMember.getUuid(), islandPartyMember);
                }
            }
        }

        islandParty.setDirty(false);
        return islandParty;
    }

    @Override
    public void saveIsland(Island island) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_islands (uuid, `name`, owner, ignore, locked, warpActive, regionVersion, schematicName, `level`, scoreMultiplier, scoreOffset, biome, leaf_breaks, hopper_count) KEY (uuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
            ps.setString(1, island.getUuid().toString());
            ps.setString(2, island.getName());
            ps.setString(3, island.getOwner().toString());
            ps.setBoolean(4, island.isIgnore());
            ps.setBoolean(5, island.isLocked());
            ps.setBoolean(6, island.isWarpActive());
            ps.setString(7, island.getRegionVersion());
            ps.setString(8, island.getSchematicName());
            ps.setDouble(9, island.getLevel());
            ps.setDouble(10, island.getScoreMultiplier());
            ps.setDouble(11, island.getScoreOffset());
            ps.setString(12, island.getBiome().getKey().toString());
            ps.setInt(13, island.getLeafBreaks());
            ps.setInt(14, island.getHopperCount());
            ps.executeUpdate();
        }

        if (island.getIslandAccessList().isDirty()) saveIslandAccessList(island.getIslandAccessList());
        if (island.getIslandLimits().isDirty()) saveIslandLimits(island.getIslandLimits());
        if (island.getIslandLocations().isDirty()) saveIslandLocations(island.getIslandLocations());
        if (island.getIslandLog().isDirty()) saveIslandLog(island.getIslandLog());
        if (island.getIslandParty().isDirty()) saveIslandParty(island.getIslandParty());

        island.setDirty(false);
    }

    private void clearIslandAccess(UUID islandUuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_island_access WHERE island_uuid = ?")) {
            ps.setString(1, islandUuid.toString());
            ps.execute();
        }
    }

    private void clearIslandLimits(UUID islandUuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_island_limits WHERE island_uuid = ?;")) {
            ps.setString(1, islandUuid.toString());
            ps.executeUpdate();
        }
    }

    private void clearIslandLocations(UUID islandUuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_island_locations WHERE island_uuid = ?")) {
            ps.setString(1, islandUuid.toString());
            ps.execute();
        }
    }

    private void clearIslandLog(UUID islandUuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_island_log WHERE island_uuid = ?")) {
            ps.setString(1, islandUuid.toString());
            ps.execute();
        }
    }

    private void clearIslandPartyMembers(UUID islandUuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_island_members WHERE island_uuid = ?")) {
            ps.setString(1, islandUuid.toString());
            ps.executeUpdate();
        }

        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_island_member_permissions WHERE island_uuid = ?;")) {
            ps.setString(1, islandUuid.toString());
            ps.executeUpdate();
        }
    }

    private void saveIslandAccessList(IslandAccessList islandAccessList) throws SQLException {
        clearIslandAccess(islandAccessList.getIsland().getUuid());

        for (IslandAccess islandAccess : islandAccessList.getAcl().values()) {
            saveIslandAccess(islandAccessList.getIsland().getUuid(), islandAccess);
        }

        islandAccessList.setDirty(false);
    }

    private void saveIslandAccess(UUID islandUuid, IslandAccess islandAccess) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_island_access (player_uuid, island_uuid, access_type) KEY (player_uuid, island_uuid) VALUES (?, ?, ?);")) {
            ps.setString(1, islandAccess.getPlayerUuid().toString());
            ps.setString(2, islandUuid.toString());
            ps.setString(3, islandAccess.getAccessType().toString());
            ps.execute();
        }
    }

    private void saveIslandLimits(IslandLimits islandLimits) throws SQLException {
        clearIslandLimits(islandLimits.getIsland().getUuid());
        saveIslandBlockLimits(islandLimits.getIsland().getUuid(), islandLimits.getBlockLimits());
        saveIslandPluginLimits(islandLimits.getIsland().getUuid(), islandLimits.getPluginLimits());
    }

    private void saveIslandBlockLimits(UUID islandUuid, Map<Material, Integer> blockLimits) throws SQLException {
        Connection c = getConnection();
        for (Map.Entry<Material, Integer> blockLimit : blockLimits.entrySet()) {
            try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_island_limits (island_uuid, entity_type, entity, `limit`) KEY (island_uuid, entity_type, entity) VALUES (?, ?, ?, ?);")) {
                ps.setString(1, islandUuid.toString());
                ps.setString(2, "BLOCK");
                ps.setString(3, blockLimit.getKey().toString());
                ps.setInt(4, blockLimit.getValue());
                ps.executeUpdate();
            }
        }
    }

    private void saveIslandPluginLimits(UUID islandUuid, Map<String, Integer> pluginLimits) throws SQLException {
        Connection c = getConnection();
        for (Map.Entry<String, Integer> pluginLimit : pluginLimits.entrySet()) {
            try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_island_limits (island_uuid, entity_type, entity, `limit`) KEY (island_uuid, entity_type, entity) VALUES (?, ?, ?, ?);")) {
                ps.setString(1, islandUuid.toString());
                ps.setString(2, "PLUGIN");
                ps.setString(3, pluginLimit.getKey());
                ps.setInt(4, pluginLimit.getValue());
                ps.executeUpdate();
            }
        }
    }

    private void saveIslandLocations(IslandLocations islandLocations) throws SQLException {
        clearIslandLocations(islandLocations.getIsland().getUuid());

        for (IslandLocation islandLocation : islandLocations.getLocations().values()) {
            saveIslandLocation(islandLocations.getIsland().getUuid(), islandLocation);
        }

        islandLocations.setDirty(false);
    }

    private void saveIslandLocation(UUID islandUuid, IslandLocation islandLocation) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_island_locations (island_uuid, location_type, location_world, location_x, location_y, location_z, location_pitch, location_yaw) KEY (island_uuid, location_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?);")) {
            ps.setString(1, islandUuid.toString());
            ps.setString(2, islandLocation.getLocationType().toString());
            ps.setString(3, islandLocation.getWorld());
            ps.setDouble(4, islandLocation.getX());
            ps.setDouble(5, islandLocation.getY());
            ps.setDouble(6, islandLocation.getZ());
            ps.setDouble(7, islandLocation.getPitch());
            ps.setDouble(8, islandLocation.getYaw());
            ps.executeUpdate();
        }
    }

    private void saveIslandLog(IslandLog islandLog) throws SQLException {
        clearIslandLog(islandLog.getIsland().getUuid());

        int logCounter = 0;
        for (IslandLogLine islandLogLine : islandLog.getLog()) {
            if (logCounter < plugin.getConfig().getInt("options.island.log-size", 10)) {
                saveIslandLogLine(islandLog.getIsland().getUuid(), islandLogLine);
            }
            logCounter++;
        }

        islandLog.setDirty(false);
    }

    private void saveIslandLogLine(UUID islandUuid, IslandLogLine islandLogLine) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_island_log (log_uuid, island_uuid, `timestamp`, log_line, variables) KEY (log_uuid, island_uuid) VALUES (?, ?, ?, ?, ?);")) {
            ps.setString(1, islandLogLine.getUuid().toString());
            ps.setString(2, islandUuid.toString());
            ps.setTimestamp(3, Timestamp.from(islandLogLine.getTimestamp()));
            ps.setString(4, islandLogLine.getLine());
            ps.setString(5, String.join(";", islandLogLine.getVariables()));
            ps.execute();
        }
    }

    private void saveIslandParty(IslandParty islandParty) throws SQLException {
        clearIslandPartyMembers(islandParty.getIsland().getUuid());
        for (IslandPartyMember partyMember : islandParty.getPartyMembers().values()) {
            saveIslandPartyMember(islandParty.getIsland().getUuid(), partyMember);
        }

        islandParty.setDirty(false);
    }

    private void saveIslandPartyMember(UUID islandUuid, IslandPartyMember partyMember) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_island_members (player_uuid, island_uuid, `role`) KEY (player_uuid) VALUES (?, ?, ?);")) {
            ps.setString(1, partyMember.getUuid().toString());
            ps.setString(2, islandUuid.toString());
            ps.setString(3, partyMember.getRole().toString());
            ps.executeUpdate();
        }

        for (String node : partyMember.getPermissions()) {
            try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_island_member_permissions (player_uuid, island_uuid, node) KEY (player_uuid, island_uuid, node) VALUES (?, ?, ?);")) {
                ps.setString(1, partyMember.getUuid().toString());
                ps.setString(2, islandUuid.toString());
                ps.setString(3, node);
                ps.execute();
            }
        }
    }

    @Override
    public void deleteIsland(Island island) throws SQLException {
        clearIslandAccess(island.getUuid());
        clearIslandLimits(island.getUuid());
        clearIslandLocations(island.getUuid());
        clearIslandLog(island.getUuid());
        clearIslandPartyMembers(island.getUuid());
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_islands WHERE uuid = ?;")) {
            ps.setString(1, island.getUuid().toString());
            ps.executeUpdate();
        }
    }

    @Override
    public Set<IslandLevel> getIslandTop(double levelCutOff) throws SQLException {
        Set<IslandLevel> islandLevels = new ConcurrentSkipListSet<>();

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT i.uuid, i.name, i.level, GROUP_CONCAT(p.username SEPARATOR ',') AS members, (SELECT p.username FROM usb_island_members m_sub JOIN usb_players p ON m_sub.player_uuid = p.uuid WHERE m_sub.island_uuid = i.uuid AND m_sub.role = 'LEADER' LIMIT 1) AS leader FROM usb_islands i LEFT JOIN usb_island_members m ON i.uuid = m.island_uuid LEFT JOIN usb_players p ON m.player_uuid = p.uuid WHERE i.ignore = FALSE AND i.level >= ? GROUP BY i.uuid ORDER BY i.level DESC;")) {
            ps.setDouble(1, levelCutOff);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String members = rs.getString("members");
                    List<String> memberList = (members != null) ? Arrays.asList(members.split(",")) : new ArrayList<>();

                    IslandLevel islandLevel = new IslandLevel(
                        rs.getString("name"),
                        rs.getString("leader"),
                        memberList,
                        rs.getDouble("level")
                    );
                    islandLevels.add(islandLevel);
                }
            }
        }

        return islandLevels;
    }

    @Override
    public Player getPlayer(UUID uuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, username, display_name FROM usb_players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Player player = new Player(
                        UUID.fromString(
                            rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getString("display_name"));

                    player.setPlayerLocations(getPlayerLocations(player));
                    player.setPendingOperations(getPendingPlayerOperations(player));
                    player.setPlayerPermissions(getPlayerPermissions(player));

                    return player;
                }
            }
        }

        return null;
    }

    @Override
    public Player getPlayer(String username) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid FROM usb_players WHERE username = ?;")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    return getPlayer(uuid);
                }
            }
        }

        return null;
    }

    @Override
    public List<String> getPlayerBannedOn(UUID playerUuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT name  FROM usb_islands i LEFT JOIN usb_island_access a ON i.uuid = a.island_uuid WHERE a.player_uuid = ? AND a.access_type = 'BANNED'")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<String> bannedOn = new ArrayList<>();
                while (rs.next()) {
                    bannedOn.add(rs.getString("name"));
                }
                return bannedOn;
            }
        }
    }

    @Override
    public List<String> getPlayerTrustedOn(UUID playerUuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT name  FROM usb_islands i LEFT JOIN usb_island_access a ON i.uuid = a.island_uuid WHERE a.player_uuid = ? AND a.access_type = 'TRUSTED'")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<String> bannedOn = new ArrayList<>();
                while (rs.next()) {
                    bannedOn.add(rs.getString("name"));
                }
                return bannedOn;
            }
        }
    }

    @Override
    public UUID getPlayerIsland(UUID playerUuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT island_uuid FROM usb_island_members WHERE player_uuid = ?;")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("island_uuid"));
                }
            }
        }

        return null;
    }

    private PlayerLocations getPlayerLocations(Player player) throws SQLException {
        PlayerLocations locations = new PlayerLocations(player);

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, location_type, location_world, location_x, location_y, location_z, location_pitch, location_yaw FROM usb_player_locations WHERE uuid = ?;")) {
            ps.setString(1, player.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PlayerLocation playerLocation = new PlayerLocation(
                        PlayerLocation.LocationType.valueOf(rs.getString("location_type")),
                        rs.getString("location_world"),
                        rs.getDouble("location_x"),
                        rs.getDouble("location_y"),
                        rs.getDouble("location_z"),
                        rs.getDouble("location_pitch"),
                        rs.getDouble("location_yaw")
                    );

                    locations.addLocation(playerLocation.getLocationType(), playerLocation);
                }
            }
        }

        locations.setDirty(false);
        return locations;
    }

    private PlayerPermissions getPlayerPermissions(Player player) throws SQLException {
        PlayerPermissions permissions = new PlayerPermissions(player);

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, `value` FROM usb_player_permissions WHERE uuid = ?;")) {
            ps.setString(1, player.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PlayerPermission permission = new PlayerPermission(rs.getString("value"));
                    permissions.addPermission(permission);
                }
            }
        }

        permissions.setDirty(false);
        return permissions;
    }

    private PendingPlayerOperations getPendingPlayerOperations(Player player) throws SQLException {
        PendingPlayerOperations pendingOperations = new PendingPlayerOperations(player);

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, `type`, `value` FROM usb_player_pending WHERE uuid = ?;")) {
            ps.setString(1, player.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PendingPlayerOperation pendingOperation = new PendingPlayerOperation(
                        PendingPlayerOperation.OperationType.valueOf(rs.getString("type")),
                        rs.getString("value"));

                    pendingOperations.addPendingOperation(pendingOperation);
                }
            }
        }

        pendingOperations.setDirty(false);
        return pendingOperations;
    }

    @Override
    public void savePlayer(Player player) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_players (uuid, username, display_name) KEY (uuid) VALUES (?, ?, ?);")) {
            ps.setString(1, player.getUuid().toString());
            ps.setString(2, player.getName());
            ps.setString(3, player.getDisplayName());
            ps.execute();
        }

        if (player.getPlayerLocations().isDirty()) savePlayerLocations(player.getPlayerLocations());
        if (player.getPlayerPendingOperations().isDirty()) savePlayerPendingOperations(player.getPlayerPendingOperations());
        if (player.getPlayerPermissions().isDirty()) savePlayerPermissions(player.getPlayerPermissions());
        player.setDirty(false);
    }

    private void clearPlayerLocations(UUID uuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_player_locations WHERE uuid = ?;")) {
            ps.setString(1, uuid.toString());
            ps.execute();
        }
    }

    private void savePlayerLocations(PlayerLocations playerLocations) throws SQLException {
        clearPlayerLocations(playerLocations.getPlayer().getUuid());

        for (PlayerLocation playerLocation : playerLocations.getLocations().values()) {
            savePlayerLocation(playerLocations.getPlayer().getUuid(), playerLocation);
        }

        playerLocations.setDirty(false);
    }

    private void savePlayerLocation(UUID uuid, PlayerLocation playerLocation) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_player_locations (`uuid`, location_type, location_world, location_x, location_y, location_z, location_pitch, location_yaw) KEY (`uuid`, location_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?);")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, playerLocation.getLocationType().toString());
            ps.setString(3, playerLocation.getWorld());
            ps.setDouble(4, playerLocation.getX());
            ps.setDouble(5, playerLocation.getY());
            ps.setDouble(6, playerLocation.getZ());
            ps.setDouble(7, playerLocation.getPitch());
            ps.setDouble(8, playerLocation.getYaw());
            ps.execute();
        }
    }

    private void clearPlayerPendingOperations(UUID playerUuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_player_pending WHERE uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.execute();
        }
    }

    private void savePlayerPendingOperations(PendingPlayerOperations pendingOperations) throws SQLException {
        clearPlayerPendingOperations(pendingOperations.getPlayer().getUuid());

        for (PendingPlayerOperation pendingOperation : pendingOperations.getPendingOperations()) {
            savePlayerPendingOperation(pendingOperations.getPlayer().getUuid(), pendingOperation);
        }

        pendingOperations.setDirty(false);
    }

    private void savePlayerPendingOperation(UUID uuid, PendingPlayerOperation operation) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO usb_player_pending (uuid, `type`, `value`) VALUES (?, ?, ?);")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, operation.getOperationType().toString());
            ps.setString(3, operation.getValue());
            ps.execute();
        }
    }

    private void clearPlayerPermissions(UUID uuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_player_permissions WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.execute();
        }
    }

    private void savePlayerPermissions(PlayerPermissions permissions) throws SQLException {
        clearPlayerPermissions(permissions.getPlayer().getUuid());

        for (PlayerPermission permission : permissions.getPermissions()) {
            savePlayerPermission(permissions.getPlayer().getUuid(), permission);
        }

        permissions.setDirty(false);
    }

    private void savePlayerPermission(UUID uuid, PlayerPermission permission) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_player_permissions (uuid, `value`) KEY (uuid, `value`) VALUES (?, ?, ?);")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, permission.getValue());
            ps.execute();
        }
    }

    @Override
    public void clearPlayer(UUID uuid) throws SQLException {
        clearPlayerLocations(uuid);
        clearPlayerPendingOperations(uuid);
    }
}
