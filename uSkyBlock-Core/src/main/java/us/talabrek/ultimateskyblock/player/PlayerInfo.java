package us.talabrek.ultimateskyblock.player;

import org.apache.commons.lang3.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.model.IslandLocation;
import us.talabrek.ultimateskyblock.api.model.PendingPlayerOperation;
import us.talabrek.ultimateskyblock.api.model.PendingPlayerOperations;
import us.talabrek.ultimateskyblock.api.model.PlayerLocation;
import us.talabrek.ultimateskyblock.api.model.PlayerPermission;
import us.talabrek.ultimateskyblock.challenge.Challenge;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletion;
import us.talabrek.ultimateskyblock.hook.permissions.PermissionsHook;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

public class PlayerInfo implements us.talabrek.ultimateskyblock.api.PlayerInfo {
    private final uSkyBlock plugin;
    private final String playerName;
    private String displayName;
    private final UUID uuid;

    private boolean islandGenerating = false;

    public PlayerInfo(us.talabrek.ultimateskyblock.api.model.Player player, uSkyBlock plugin) {
        this.plugin = plugin;
        this.uuid = player.getUuid();
        this.playerName = player.getName();
    }

    public void startNewIsland(final Location l) {
        getDatabasePlayer().getPlayerLocations().removeLocation(PlayerLocation.LocationType.HOME);
        save();
    }

    public void removeFromIsland() {
        getDatabasePlayer().getPlayerLocations().removeLocation(PlayerLocation.LocationType.HOME);
        islandGenerating = false;
        save();
    }

    @Override
    public boolean getHasIsland() {
        return plugin.getStorage().getPlayerIsland(getUniqueId()).join() != null;
    }

    public String locationForParty() {
        return LocationUtil.getIslandName(getIslandLocation());
    }

    @Override
    public Player getPlayer() {
        Player player = null;
        if (uuid != null) {
            player = uSkyBlock.getInstance().getPlayerDB().getPlayer(uuid);
        }
        if (player == null && playerName != null) {
            player = uSkyBlock.getInstance().getPlayerDB().getPlayer(playerName);
        }
        return player;
    }

    public OfflinePlayer getOfflinePlayer() {
        if (uuid != null) {
            return uSkyBlock.getInstance().getPlayerDB().getOfflinePlayer(uuid);
        }
        return null;
    }

    @Override
    public String getPlayerName() {
        return this.playerName;
    }

    public UUID getPlayerId() {
        return this.uuid;
    }

    @Override
    public Location getIslandLocation() {
        UUID islandUuid = plugin.getStorage().getPlayerIsland(getUniqueId()).join();
        if (islandUuid == null) {
            return null;
        }

        return plugin.getIslandLogic()
            .getIsland(islandUuid)
            .getIslandLocations()
            .getLocation(IslandLocation.LocationType.CENTER_WORLD)
            .asBukkitLocation();
    }

    @Override
    public Location getIslandNetherLocation() {
        UUID islandUuid = plugin.getStorage().getPlayerIsland(getUniqueId()).join();
        if (islandUuid == null) {
            return null;
        }

        IslandLocation netherLocation = plugin.getIslandLogic()
            .getIsland(islandUuid)
            .getIslandLocations()
            .getLocation(IslandLocation.LocationType.CENTER_NETHER);

        if (netherLocation == null) {
            return null;
        }

        return netherLocation.asBukkitLocation();
    }

    public void setHomeLocation(final Location l) {
        if (l == null) {
            getDatabasePlayer().getPlayerLocations().removeLocation(PlayerLocation.LocationType.HOME);
        } else {
            PlayerLocation homeLocation = new PlayerLocation(PlayerLocation.LocationType.HOME, l);
            getDatabasePlayer().getPlayerLocations().addLocation(PlayerLocation.LocationType.HOME, homeLocation);
        }
        save();
    }

    @Override
    public Location getHomeLocation() {
        PlayerLocation homeLocation = getDatabasePlayer().getPlayerLocations().getLocation(PlayerLocation.LocationType.HOME);

        if (homeLocation == null) {
            return null;
        }

        return homeLocation.asBukkitLocation();
    }

    @Override
    public String getDisplayName() {
        return displayName != null ? displayName : playerName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        getDatabasePlayer().setDisplayName(displayName);
        save();
    }

    public void setJoinParty(final Location l) {
        this.setHomeLocation(l);
    }

    public void completeChallenge(Challenge challenge, boolean silent) {
        uSkyBlock.getInstance().getChallengeLogic().completeChallenge(this, challenge.getName());
        if (silent) {
            return;
        }
        IslandInfo island = getIslandInfo();
        if (island != null) {
            island.sendMessageToOnlineMembers(tr("\u00a79{0}\u00a7f has completed the \u00a79{1}\u00a7f challenge!",
                getPlayerName(), challenge.getDisplayName()));
        }
    }

    public void resetChallenge(final String challenge) {
        uSkyBlock.getInstance().getChallengeLogic().resetChallenge(this, challenge);
    }

    public int checkChallenge(final String challenge) {
        return uSkyBlock.getInstance().getChallengeLogic().checkChallenge(this, challenge);
    }

    public ChallengeCompletion getChallenge(final String challenge) {
        return uSkyBlock.getInstance().getChallengeLogic().getChallenge(this, challenge);
    }

    public void resetAllChallenges() {
        uSkyBlock.getInstance().getChallengeLogic().resetAllChallenges(this);
    }

    public void save() {
        plugin.getStorage().savePlayer(getDatabasePlayer());
    }

    @Override
    public Collection<us.talabrek.ultimateskyblock.api.ChallengeCompletion> getChallenges() {
        Collection<us.talabrek.ultimateskyblock.api.ChallengeCompletion> copy = new ArrayList<>();
        copy.addAll(uSkyBlock.getInstance().getChallengeLogic().getChallenges(this));
        return copy;
    }

    @Override
    public String toString() {
        // TODO: 01/06/2015 - R4zorax: use i18n.tr
        String str = "\u00a7bPlayer Info:\n";
        str += ChatColor.GRAY + "  - name: " + ChatColor.DARK_AQUA + getPlayerName() + "\n";
        str += ChatColor.GRAY + "  - nick: " + ChatColor.DARK_AQUA + getDisplayName() + "\n";
        str += ChatColor.GRAY + "  - hasIsland: " + ChatColor.DARK_AQUA + getHasIsland() + "\n";
        str += ChatColor.GRAY + "  - home: " + ChatColor.DARK_AQUA + LocationUtil.asString(getHomeLocation()) + "\n";
        str += ChatColor.GRAY + "  - island: " + ChatColor.DARK_AQUA + LocationUtil.asString(getIslandLocation()) + "\n";
        str += ChatColor.GRAY + "  - banned from: " + ChatColor.DARK_AQUA + plugin.getStorage().getPlayerBannedOn(getUniqueId()) + "\n";
        str += ChatColor.GRAY + "  - trusted on: " + ChatColor.DARK_AQUA + plugin.getStorage().getPlayerTrustedOn(getUniqueId()) + "\n";
        return str;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public List<String> getBannedFrom() {
        return plugin.getStorage().getPlayerBannedOn(getUniqueId()).join();
    }

    @Override
    public List<String> getTrustedOn() {
        return plugin.getStorage().getPlayerTrustedOn(getUniqueId()).join();
    }

    public boolean isIslandGenerating() {
        return this.islandGenerating;
    }

    public void setIslandGenerating(boolean value) {
        this.islandGenerating = value;
    }

    @Override
    public IslandInfo getIslandInfo() {
        UUID islandUuid = plugin.getStorage().getPlayerIsland(getUniqueId()).join();
        if (islandUuid == null) {
            return null;
        } else {
            return plugin.getIslandLogic().getIslandInfo(islandUuid);
        }
    }

    public void setClearInventoryOnNextEntry(boolean b) {
        getDatabasePlayer().setClearInventory(b);
        save();
    }

    public boolean isClearInventoryOnNextEntry() {
        return getDatabasePlayer().isClearInventory();
    }

    public void onTeleport(final Player player) {
        if (isClearInventoryOnNextEntry()) {
            plugin.sync(() -> plugin.clearPlayerInventory(player), 50);
        }

        PendingPlayerOperations pendingOperations = getDatabasePlayer().getPlayerPendingOperations();
        pendingOperations.getPendingOperations().forEach(operation -> {
            if (operation.getOperationType() == PendingPlayerOperation.OperationType.COMMAND) {
                plugin.execCommand(player, operation.getValue(), false);
            }

            if (operation.getOperationType() == PendingPlayerOperation.OperationType.PERMISSION) {
                addPermissions(List.of(operation.getValue()));
            }
        });
        pendingOperations.clearPendingOperations();
        save();
    }

    public boolean execCommands(@Nullable List<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return true;
        }
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            plugin.execCommands(player, commands);
            save();
            return true;
        } else {
            commands.forEach(command ->
                getDatabasePlayer().getPlayerPendingOperations().addPendingOperation(
                    new PendingPlayerOperation(PendingPlayerOperation.OperationType.COMMAND, command)));
            save();
            return false;
        }
    }

    public boolean addPermissions(@Nullable List<String> perms) {
        if (perms == null || perms.isEmpty()) {
            return true;
        }

        Player target = getPlayer();
        Optional<PermissionsHook> hook = plugin.getHookManager().getPermissionsHook();

        if (target != null && target.isOnline() && hook.isPresent()) {
            PermissionsHook pHook = hook.get();

            for (String perm : perms) {
                if (!pHook.hasPermission(target, perm)) {
                    getDatabasePlayer().getPlayerPermissions().addPermission(new PlayerPermission(perm));
                    pHook.addPermission(target, perm);
                }
            }

            save();
            return true;
        } else {
            perms.forEach(permission -> getDatabasePlayer().getPlayerPendingOperations().addPendingOperation(
                new PendingPlayerOperation(PendingPlayerOperation.OperationType.PERMISSION, permission)));
            save();
            return false;
        }
    }

    public void clearPerms(@NotNull Player target) {
        Validate.notNull(target, "Target cannot be null!");

        plugin.getHookManager().getPermissionsHook().ifPresent((hook) ->
            getDatabasePlayer().getPlayerPermissions().getPermissions().forEach(
                permission -> hook.removePermission(target, permission.getValue()))
        );

        getDatabasePlayer().getPlayerPermissions().clearPermissions();
        getDatabasePlayer().getPlayerPendingOperations().clearPendingPermissions();
        save();
    }

    private us.talabrek.ultimateskyblock.api.model.Player getDatabasePlayer() {
        return plugin.getPlayerLogic().getPlayer(uuid);
    }
}
