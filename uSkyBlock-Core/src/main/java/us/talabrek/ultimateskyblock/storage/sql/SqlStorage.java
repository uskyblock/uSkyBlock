package us.talabrek.ultimateskyblock.storage.sql;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.model.ChallengeCompletionSet;
import us.talabrek.ultimateskyblock.api.model.Island;
import us.talabrek.ultimateskyblock.api.model.Player;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class SqlStorage {
    protected final uSkyBlock plugin;

    public SqlStorage(uSkyBlock plugin) {
        this.plugin = plugin;
    }

    public abstract void initialize() throws SQLException;

    public abstract void close() throws SQLException;

    public abstract ChallengeCompletionSet getChallengeCompletion(UUID uuid) throws SQLException;

    public abstract void saveChallengeCompletion(ChallengeCompletionSet completionSet) throws SQLException;

    public abstract int deleteChallengeCompletion(UUID uuid) throws SQLException;

    public abstract @Nullable UUID getIslandByName(String name) throws SQLException;

    public abstract int getIslandCount() throws SQLException;

    public abstract @Nullable Island getIsland(UUID uuid) throws SQLException;

    public abstract @NotNull Set<UUID> getIslands() throws SQLException;

    public abstract void saveIsland(Island island) throws SQLException;

    public abstract void deleteIsland(Island island) throws SQLException;

    public abstract Player getPlayer(UUID uuid) throws SQLException;

    public abstract Player getPlayer(String username) throws SQLException;

    public abstract List<String> getPlayerBannedOn(UUID playerUuid) throws SQLException;

    public abstract List<String> getPlayerTrustedOn(UUID playerUuid) throws SQLException;

    public abstract UUID getPlayerIsland(UUID playerUuid) throws SQLException;

    public abstract void savePlayer(Player player) throws SQLException;

    public abstract void clearPlayer(UUID uuid) throws SQLException;
}
