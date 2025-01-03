package us.talabrek.ultimateskyblock.storage.sql;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import us.talabrek.ultimateskyblock.api.model.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        return "jdbc:h2:" + databaseFile.toAbsolutePath() + ";MODE=MariaDB;DATABASE_TO_LOWER=TRUE";
    }

    @Override
    public PlayerInfo getPlayerInfo(UUID uuid) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, username, display_name FROM usb_players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerInfo(
                        UUID.fromString(
                            rs.getString("uuid")),
                            rs.getString("username"),
                            rs.getString("display_name"));
                }
            }
        }

        return null;
    }

    @Override
    public PlayerInfo getPlayerInfo(String username) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, username, display_name FROM usb_players WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerInfo(
                        UUID.fromString(
                            rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getString("display_name"));
                }
            }
        }

        return null;
    }

    @Override
    public void savePlayerInfo(PlayerInfo playerInfo) throws SQLException {
        Connection c = getConnection();
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO usb_players (uuid, username, display_name) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE username=VALUES(username), display_name=VALUES(display_name)")) {
            ps.setString(1, playerInfo.getUuid().toString());
            ps.setString(2, playerInfo.getName());
            ps.setString(3, playerInfo.getDisplayName());
            ps.execute();
        }
    }
}
