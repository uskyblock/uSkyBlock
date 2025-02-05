package us.talabrek.ultimateskyblock.storage.sql;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.model.CenterLocation;
import us.talabrek.ultimateskyblock.api.model.ChallengeCompletion;
import us.talabrek.ultimateskyblock.api.model.Island;
import us.talabrek.ultimateskyblock.api.model.IslandAccess;
import us.talabrek.ultimateskyblock.api.model.IslandAccessList;
import us.talabrek.ultimateskyblock.api.model.IslandLocation;
import us.talabrek.ultimateskyblock.api.model.IslandLocations;
import us.talabrek.ultimateskyblock.api.model.IslandLog;
import us.talabrek.ultimateskyblock.api.model.IslandLogLine;
import us.talabrek.ultimateskyblock.api.model.IslandParty;
import us.talabrek.ultimateskyblock.api.model.IslandPartyMember;
import us.talabrek.ultimateskyblock.api.model.PendingPlayerOperation;
import us.talabrek.ultimateskyblock.api.model.PendingPlayerOperations;
import us.talabrek.ultimateskyblock.api.model.Player;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

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
            plugin.getLogger().info("Connecting to " + getUrl());
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
    public void saveChallengeCompletion(ChallengeCompletion challengeCompletion) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_challenge_completion (uuid, sharing_type, challenge, first_completed, times_completed, times_completed_since_timer) KEY (uuid, sharing_type, challenge) VALUES (?, ?, ?, ?, ?, ?);")) {
            ps.setString(1, challengeCompletion.getUuid().toString());
            ps.setString(2, challengeCompletion.getSharingType().toString());
            ps.setString(3, challengeCompletion.getChallenge());
            ps.setTimestamp(4, Timestamp.from(challengeCompletion.getFirstCompleted()));
            ps.setInt(5, challengeCompletion.getTimesCompleted());
            ps.setInt(6, challengeCompletion.getTimesCompletedSinceTimer());
            ps.execute();
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
    public @Nullable Island getIsland(UUID uuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, `name`, center_x, center_z, owner, ignore, locked, warpActive, regionVersion, schematicName, `level`, scoreMultiplier, scoreOffset, biome, leaf_breaks, hopper_count FROM usb_islands WHERE uuid = ?;")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Island island = new Island(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                    island.setOwner(UUID.fromString(rs.getString("owner")));
                    island.setLocation(new CenterLocation(rs.getInt("center_x"), rs.getInt("center_z")));
                    island.setIgnore(rs.getBoolean("ignore"));
                    island.setLocked(rs.getBoolean("locked"));
                    island.setWarpActive(rs.getBoolean("warpActive"));
                    island.setRegionVersion(rs.getString("regionVersion"));
                    island.setSchematicName(rs.getString("schematicName"));

                    island.setLevel(rs.getDouble("level"));
                    island.setScoreMultiplier(rs.getDouble("scoreMultiplier"));
                    island.setScoreOffset(rs.getDouble("scoreOffset"));

                    island.setBiome(rs.getString("biome"));
                    island.setLeafBreaks(rs.getInt("leaf_breaks"));
                    island.setHopperCount(rs.getInt("hopper_count"));

                    island.setIslandAccessList(getIslandAccessList(island));
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

    private IslandAccessList getIslandAccessList(Island island) throws SQLException {
        IslandAccessList acl = new IslandAccessList(island);

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT player_uuid, island_uuid, access_type FROM usb_island_access WHERE island_uuid = ?")) {
            ps.setString(1, island.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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

    private IslandLocations getIslandLocations(Island island) throws SQLException {
        IslandLocations locations = new IslandLocations(island);

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT island_uuid, location_type, location_world, location_x, location_y, location_z, location_pitch, location_yaw FROM usb_island_locations WHERE island_uuid = ?")) {
            ps.setString(1, island.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
                if (rs.next()) {
                    IslandLogLine logLine = new IslandLogLine(
                        UUID.fromString(rs.getString("log_uuid")),
                        Instant.ofEpochSecond(rs.getLong("timestamp")),
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
        try (PreparedStatement ps = c.prepareStatement("SELECT player_uuid, island_uuid, `role`, can_change_biome, can_toggle_lock, can_change_warp, can_toggle_warp, can_invite_others, can_kick_others, can_ban_others, max_animals, max_monsters, max_villagers, max_golems FROM usb_island_members WHERE island_uuid = ?;")) {
            ps.setString(1, island.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    IslandPartyMember islandPartyMember = new IslandPartyMember(
                        UUID.fromString(rs.getString("player_uuid")),
                        IslandPartyMember.Role.valueOf(rs.getString("role")),
                        rs.getBoolean("can_change_biome"),
                        rs.getBoolean("can_toggle_lock"),
                        rs.getBoolean("can_change_warp"),
                        rs.getBoolean("can_toggle_warp"),
                        rs.getBoolean("can_invite_others"),
                        rs.getBoolean("can_kick_others"),
                        rs.getBoolean("can_ban_others"),
                        rs.getInt("max_animals"),
                        rs.getInt("max_monsters"),
                        rs.getInt("max_villagers"),
                        rs.getInt("max_golems"));

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
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_islands (uuid, `name`, center_x, center_z, owner, ignore, locked, warpActive, regionVersion, schematicName, `level`, scoreMultiplier, scoreOffset, biome, leaf_breaks, hopper_count) KEY (uuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
            ps.setString(1, island.getUuid().toString());
            ps.setString(2, island.getName());
            ps.setInt(3, island.getLocation().getX());
            ps.setInt(4, island.getLocation().getZ());
            ps.setString(5, island.getOwner().toString());
            ps.setBoolean(6, island.isIgnore());
            ps.setBoolean(7, island.isLocked());
            ps.setBoolean(8, island.isWarpActive());
            ps.setString(9, island.getRegionVersion());
            ps.setString(10, island.getSchematicName());
            ps.setDouble(11, island.getLevel());
            ps.setDouble(12, island.getScoreMultiplier());
            ps.setDouble(13, island.getScoreOffset());
            ps.setString(14, island.getBiome());
            ps.setInt(15, island.getLeafBreaks());
            ps.setInt(16, island.getHopperCount());
            ps.execute();
        }

        if (island.getIslandAccessList().isDirty()) saveIslandAccessList(island.getIslandAccessList());
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
            ps.execute();
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
            ps.execute();
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
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_island_log (log_uuid, island_uuid, `timestamp`, log_line, variables) KEY (island_uuid) VALUES (?, ?, ?, ?, ?);")) {
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
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_island_members (player_uuid, island_uuid, `role`, can_change_biome, can_toggle_lock, can_change_warp, can_toggle_warp, can_invite_others, can_kick_others, can_ban_others, max_animals, max_monsters, max_villagers, max_golems) KEY (player_uuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
            ps.setString(1, partyMember.getUuid().toString());
            ps.setString(2, islandUuid.toString());
            ps.setString(3, partyMember.getRole().toString());
            ps.setBoolean(4, partyMember.isCanChangeBiome());
            ps.setBoolean(5, partyMember.isCanToggleLock());
            ps.setBoolean(6, partyMember.isCanChangeWarp());
            ps.setBoolean(7, partyMember.isCanToggleWarp());
            ps.setBoolean(8, partyMember.isCanInviteOthers());
            ps.setBoolean(9, partyMember.isCanKickOthers());
            ps.setBoolean(10, partyMember.isCanBanOthers());
            ps.setInt(11, partyMember.getMaxAnimals());
            ps.setInt(12, partyMember.getMaxMonsters());
            ps.setInt(13, partyMember.getMaxVillagers());
            ps.setInt(14, partyMember.getMaxGolems());
            ps.execute();
        }
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

                    player.setPendingOperations(getPendingPlayerOperations(player));

                    return player;
                }
            }
        }

        return null;
    }

    @Override
    public Player getPlayer(String username) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, username, display_name FROM usb_players WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Player player = new Player(
                        UUID.fromString(
                            rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getString("display_name"));

                    player.setPendingOperations(getPendingPlayerOperations(player));

                    return player;
                }
            }
        }

        return null;
    }

    private PendingPlayerOperations getPendingPlayerOperations(Player player) throws SQLException {
        PendingPlayerOperations pendingOperations = new PendingPlayerOperations(player);

        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, `type`, `value` FROM usb_player_pending WHERE uuid = ?;")) {
            ps.setString(1, player.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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

        if (player.getPendingOperations().isDirty()) savePendingPlayerOperations(player.getPendingOperations());
        player.setDirty(false);
    }

    private void clearPendingPlayerOperations(UUID playerUuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM usb_player_pending WHERE uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.execute();
        }
    }

    private void savePendingPlayerOperations(PendingPlayerOperations pendingOperations) throws SQLException {
        clearPendingPlayerOperations(pendingOperations.getPlayer().getUuid());

        for (PendingPlayerOperation pendingOperation : pendingOperations.getPendingOperations()) {
            savePendingPlayerOperation(pendingOperations.getPlayer().getUuid(), pendingOperation);
        }

        pendingOperations.setDirty(false);
    }

    private void savePendingPlayerOperation(UUID uuid, PendingPlayerOperation operation) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO usb_player_pending (uuid, `type`, `value`) KEY (uuid) VALUES (?, ?, ?);")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, operation.getOperationType().toString());
            ps.setString(3, operation.getValue());
            ps.execute();
        }
    }
}
