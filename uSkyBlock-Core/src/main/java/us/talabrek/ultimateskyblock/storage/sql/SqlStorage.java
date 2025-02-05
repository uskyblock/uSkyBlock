package us.talabrek.ultimateskyblock.storage.sql;

import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.model.ChallengeCompletion;
import us.talabrek.ultimateskyblock.api.model.Island;
import us.talabrek.ultimateskyblock.api.model.Player;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.sql.SQLException;
import java.util.UUID;

public abstract class SqlStorage {
    protected final uSkyBlock plugin;

    public SqlStorage(uSkyBlock plugin) {
        this.plugin = plugin;
    }

    public abstract void initialize() throws SQLException;

    public abstract void close() throws SQLException;

    public abstract void saveChallengeCompletion(ChallengeCompletion challengeCompletion) throws SQLException;

    public abstract @Nullable UUID getIslandByName(String name) throws SQLException;

    public abstract @Nullable Island getIsland(UUID uuid) throws SQLException;

    public abstract void saveIsland(Island island) throws SQLException;

    public abstract Player getPlayer(UUID uuid) throws SQLException;

    public abstract Player getPlayer(String username) throws SQLException;

    public abstract void savePlayer(Player player) throws SQLException;
}
