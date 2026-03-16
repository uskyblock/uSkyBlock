package us.talabrek.ultimateskyblock.challenge;

import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.island.IslandKey;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqliteChallengeProgressRepository implements ChallengeProgressRepository {
    private static final String SCHEMA_VERSION = "1";

    private final Logger logger;
    private final String jdbcUrl;

    public SqliteChallengeProgressRepository(@NotNull Path dbPath, @NotNull Logger logger) {
        this.logger = logger;
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        try {
            java.nio.file.Files.createDirectories(dbPath.toAbsolutePath().getParent());
            initialize();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize challenge progress database at " + dbPath, e);
        }
    }

    @Override
    public @NotNull Map<ChallengeKey, ChallengeCompletion> load(@NotNull IslandKey islandKey) {
        Map<ChallengeKey, ChallengeCompletion> progress = new HashMap<>();
        String sql = """
            SELECT challenge_id, cooldown_until_ms, times_completed, times_completed_in_window
            FROM challenge_progress
            WHERE island_key = ?
            """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, islandKey.value());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long cooldownUntil = rs.getObject("cooldown_until_ms", Long.class);
                    progress.put(
                        ChallengeKey.of(rs.getString("challenge_id")),
                        new ChallengeCompletion(
                            ChallengeKey.of(rs.getString("challenge_id")),
                            cooldownUntil != null ? Instant.ofEpochMilli(cooldownUntil) : null,
                            rs.getInt("times_completed"),
                            rs.getInt("times_completed_in_window")
                        )
                    );
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load challenge progress for " + islandKey.value(), e);
        }
        return progress;
    }

    @Override
    public void replace(@NotNull IslandKey islandKey, @NotNull Map<ChallengeKey, ChallengeCompletion> progress) {
        String deleteSql = "DELETE FROM challenge_progress WHERE island_key = ?";
        String insertSql = """
            INSERT INTO challenge_progress (
                island_key, challenge_id, cooldown_until_ms, times_completed, times_completed_in_window, updated_at_ms
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement(deleteSql)) {
                delete.setString(1, islandKey.value());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                long updatedAt = System.currentTimeMillis();
                for (Map.Entry<ChallengeKey, ChallengeCompletion> entry : progress.entrySet()) {
                    ChallengeCompletion completion = entry.getValue();
                    if (isDefault(completion)) {
                        continue;
                    }
                    insert.setString(1, islandKey.value());
                    insert.setString(2, entry.getKey().id());
                    if (completion.cooldownUntil() != null) {
                        insert.setLong(3, completion.cooldownUntil().toEpochMilli());
                    } else {
                        insert.setObject(3, null);
                    }
                    insert.setInt(4, completion.getTimesCompleted());
                    insert.setInt(5, completion.getTimesCompletedInCooldown());
                    insert.setLong(6, updatedAt);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to store challenge progress for " + islandKey.value(), e);
        }
    }

    @Override
    public boolean hasProgress(@NotNull IslandKey islandKey) {
        String sql = "SELECT 1 FROM challenge_progress WHERE island_key = ? LIMIT 1";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, islandKey.value());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to query challenge progress for " + islandKey.value(), e);
        }
    }

    @Override
    public void shutdown() {
        // No-op. Connections are short-lived per operation.
    }

    private @NotNull Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            logger.log(Level.FINE, "Unable to enable SQLite foreign keys", e);
        }
        return connection;
    }

    private void initialize() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS challenge_progress (
                    island_key TEXT NOT NULL,
                    challenge_id TEXT NOT NULL,
                    cooldown_until_ms BIGINT NULL,
                    times_completed INTEGER NOT NULL,
                    times_completed_in_window INTEGER NOT NULL,
                    updated_at_ms BIGINT NOT NULL,
                    PRIMARY KEY (island_key, challenge_id),
                    CHECK (times_completed >= 0),
                    CHECK (times_completed_in_window >= 0)
                )
                """);
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_challenge_progress_island
                ON challenge_progress (island_key)
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS challenge_progress_meta (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """);
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO challenge_progress_meta(key, value)
                 VALUES ('schema_version', ?)
                 ON CONFLICT(key) DO UPDATE SET value=excluded.value
                 """)) {
            statement.setString(1, SCHEMA_VERSION);
            statement.executeUpdate();
        }
    }

    private static boolean isDefault(@NotNull ChallengeCompletion completion) {
        return completion.cooldownUntil() == null
            && completion.getTimesCompleted() == 0
            && completion.getTimesCompletedInCooldown() == 0;
    }
}
