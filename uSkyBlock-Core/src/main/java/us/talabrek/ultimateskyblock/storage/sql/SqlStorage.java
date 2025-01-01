package us.talabrek.ultimateskyblock.storage.sql;

import us.talabrek.ultimateskyblock.api.model.PlayerInfo;
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

    public abstract PlayerInfo getPlayerInfo(UUID uuid) throws SQLException;

    public abstract PlayerInfo getPlayerInfo(String username) throws SQLException;

    public abstract void savePlayerInfo(PlayerInfo playerInfo) throws SQLException;
}
