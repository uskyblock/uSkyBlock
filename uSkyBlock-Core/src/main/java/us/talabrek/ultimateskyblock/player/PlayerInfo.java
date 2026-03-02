package us.talabrek.ultimateskyblock.player;

import dk.lockfuglsang.minecraft.file.FileUtil;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.Validate;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.challenge.Challenge;
import us.talabrek.ultimateskyblock.hook.permissions.PermissionsHook;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.message.Placeholder;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.util.LogUtil;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.util.UUIDUtil;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.plainText;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

public class PlayerInfo implements Serializable, us.talabrek.ultimateskyblock.api.PlayerInfo {
    private static final String CN = PlayerInfo.class.getName();
    private static final Logger log = Logger.getLogger(CN);
    @Serial
    private static final long serialVersionUID = 1L;
    private static final int YML_VERSION = 1;
    private static final String PLAYER_PERMS_PATH = "player.perms";
    private static final String PENDING_PERMISSIONS_PATH = "pending-permissions";
    private static final String PLAYER_UNLOCKED_BIOMES_PATH = "player.unlocked-biomes";
    private static final String PENDING_BIOME_PERMISSION_REMOVALS_PATH = "pending-biome-permission-removals";
    private static final String BIOME_PERMISSION_PREFIX = "usb.biome.";
    private final uSkyBlock plugin;
    private final Scheduler scheduler;
    private final String playerName;
    private String displayName;
    private UUID uuid;

    private Location islandLocation;

    private Location homeLocation;

    private final FileConfiguration playerData;
    private final File playerConfigFile;

    private boolean islandGenerating = false;
    private boolean dirty = false;

    public PlayerInfo(String currentPlayerName, UUID playerUUID, uSkyBlock plugin, Path playerDataDirectory) {
        this.plugin = plugin;
        this.scheduler = plugin.getScheduler();
        this.uuid = playerUUID;
        this.playerName = currentPlayerName;
        // Prefer UUID over Name
        // TODO: decouple serialization from player data.
        // TODO: remove legacy player name support - all data should be converted by now.
        playerConfigFile = playerDataDirectory.resolve(UUIDUtil.asString(playerUUID) + ".yml").toFile();
        File nameFile = playerDataDirectory.resolve(playerName + ".yml").toFile();
        if (!playerConfigFile.exists() && nameFile.exists() && !currentPlayerName.equals(PlayerDB.UNKNOWN_PLAYER_NAME)) {
            nameFile.renameTo(playerConfigFile);
        }
        playerData = new YamlConfiguration();
        if (playerConfigFile.exists()) {
            FileUtil.readConfig(playerData, playerConfigFile);
        }
        loadPlayer();
    }

    public void startNewIsland(final Location l) {
        this.setIslandLocation(l);
        this.homeLocation = null;
    }

    public void removeFromIsland() {
        this.setIslandLocation(null);
        this.homeLocation = null;
        islandGenerating = false;
    }

    @Override
    public boolean getHasIsland() {
        return getIslandLocation() != null;
    }

    public String locationForParty() {
        return LocationUtil.getIslandName(this.islandLocation);
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

    public void setIslandLocation(final Location l) {
        this.islandLocation = l != null ? l.clone() : null;
    }

    @Override
    public Location getIslandLocation() {
        return islandLocation != null && islandLocation.getBlockY() != 0 ? islandLocation.clone() : null;
    }

    @Override
    public Location getIslandNetherLocation() {
        Location l = getIslandLocation();
        World nether = uSkyBlock.getInstance().getWorldManager().getNetherWorld();
        if (nether == null) {
            return null;
        }
        if (l != null) {
            l.setWorld(nether);
            l.setY(l.getY() / 2);
        }
        return l;
    }

    public void setHomeLocation(final Location l) {
        this.homeLocation = l != null ? l.clone() : null;
    }

    @Override
    public Location getHomeLocation() {
        return homeLocation != null ? homeLocation.clone() : null;
    }

    @Override
    public String getDisplayName() {
        return displayName != null ? displayName : playerName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setJoinParty(final Location l) {
        this.islandLocation = l != null ? l.clone() : null;
        // TODO: 09/09/2015 - R4zorax: Use the leaders home instead
        this.homeLocation = l != null ? l.clone() : null;
    }

    public void completeChallenge(Challenge challenge, boolean silent) {
        uSkyBlock.getInstance().getChallengeLogic().completeChallenge(this, challenge.getId());
        if (silent) {
            return;
        }
        IslandInfo island = getIslandInfo();
        if (island != null) {
            island.sendMessageToOnlineMembers(trLegacy("<player> has completed the <challenge> challenge!",
                unparsed("player", getPlayerName(), PRIMARY),
                Placeholder.legacy("challenge", challenge.getDisplayName(), PRIMARY)));
        }
    }

    private void setupPlayer() {
        FileConfiguration playerConfig = playerData;
        ConfigurationSection pSection = playerConfig.createSection("player");
        pSection.set("islandX", 0);
        pSection.set("islandY", 0);
        pSection.set("islandZ", 0);
        pSection.set("homeX", 0);
        pSection.set("homeY", 0);
        pSection.set("homeZ", 0);
        pSection.set("homeYaw", 0);
        pSection.set("homePitch", 0);
        pSection.set("perms", null);
        pSection.set("unlocked-biomes", null);
    }

    private PlayerInfo loadPlayer() {
        if (!playerData.contains("player.islandY") || playerData.getInt("player.islandY", 0) == 0) {
            this.islandLocation = null;
            this.homeLocation = null;
            createPlayerConfig();
            return this;
        }
        try {
            this.displayName = playerData.getString("player.displayName", playerName);
            this.uuid = UUIDUtil.fromString(playerData.getString("player.uuid", null));
            this.islandLocation = new Location(uSkyBlock.getInstance().getWorldManager().getWorld(),
                playerData.getInt("player.islandX"), playerData.getInt("player.islandY"), playerData.getInt("player.islandZ"));
            this.homeLocation = new Location(uSkyBlock.getInstance().getWorldManager().getWorld(),
                playerData.getInt("player.homeX") + 0.5, playerData.getInt("player.homeY") + 0.2, playerData.getInt("player.homeZ") + 0.5,
                (float) playerData.getDouble("player.homeYaw", 0.0),
                (float) playerData.getDouble("player.homePitch", 0.0));

            migrateLegacyBiomePermissions();
            processPendingBiomePermissionRemovals(getPlayer());

            log.exiting(CN, "loadPlayer");
            return this;
        } catch (Exception e) {
            LogUtil.log(Level.INFO, "Returning null while loading, not good!");
            return null;
        }
    }

    private void createPlayerConfig() {
        LogUtil.log(Level.FINER, "Creating new player config!");
        setupPlayer();
    }

    public FileConfiguration getConfig() {
        return playerData;
    }

    public void save() {
        dirty = true;
        if (!playerConfigFile.exists()) {
            saveToFile();
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void saveToFile() {
        log.fine("Saving player-info for " + playerName + " to file");
        log.entering(CN, "save", playerName);
        if (playerData == null) {
            LogUtil.log(Level.INFO, "Can't save player data! (" + playerName + ", " + uuid + ", " + playerConfigFile + ")");
            return;
        }
        FileConfiguration playerConfig = playerData;
        playerConfig.set("version", YML_VERSION);
        playerConfig.set("player.hasIsland", null); // Remove it (deprecated)
        playerConfig.set("player.displayName", displayName);
        playerConfig.set("player.uuid", UUIDUtil.asString(uuid));
        Location location = this.getIslandLocation();
        if (location != null) {
            playerConfig.set("player.islandX", location.getBlockX());
            playerConfig.set("player.islandY", location.getBlockY());
            playerConfig.set("player.islandZ", location.getBlockZ());
        } else {
            playerConfig.set("player.islandX", 0);
            playerConfig.set("player.islandY", 0);
            playerConfig.set("player.islandZ", 0);
        }
        Location home = this.getHomeLocation();
        if (home != null) {
            playerConfig.set("player.homeX", home.getBlockX());
            playerConfig.set("player.homeY", home.getBlockY());
            playerConfig.set("player.homeZ", home.getBlockZ());
            playerConfig.set("player.homeYaw", home.getYaw());
            playerConfig.set("player.homePitch", home.getPitch());
        } else {
            playerConfig.set("player.homeX", 0);
            playerConfig.set("player.homeY", 0);
            playerConfig.set("player.homeZ", 0);
            playerConfig.set("player.homeYaw", 0);
            playerConfig.set("player.homePitch", 0);
        }
        try {
            playerConfig.save(playerConfigFile);
            LogUtil.log(Level.FINEST, "Player data saved!");
        } catch (IOException ex) {
            uSkyBlock.getInstance().getLogger().log(Level.SEVERE, "Could not save config to " + playerConfigFile, ex);
        }
        log.exiting(CN, "save");
        dirty = false;
    }

    @Override
    public Collection<us.talabrek.ultimateskyblock.api.ChallengeCompletion> getChallenges() {
        return new ArrayList<>(uSkyBlock.getInstance().getChallengeLogic().getChallenges(this));
    }

    @Override
    public String toString() {
        StringBuilder plain = new StringBuilder();
        for (Component line : asComponentLines()) {
            if (plain.length() > 0) {
                plain.append('\n');
            }
            plain.append(plainText(line));
        }
        return plain.toString();
    }

    public @NotNull Component[] asComponentLines() {
        String bannedFrom = String.join(", ", getBannedFrom());
        String trustedOn = String.join(", ", playerData.getStringList("trustedOn"));
        return new Component[] {
            // I18N: Header line for the admin player info debug output.
            tr("Player Info:", PRIMARY),
            // I18N: Label for the account name in admin player info debug output.
            tr("  - name: <name>", MUTED, unparsed("name", getPlayerName(), PRIMARY)),
            // I18N: Label for the display name / nickname in admin player info debug output.
            tr("  - nick: <nick>", MUTED, Placeholder.legacy("nick", getDisplayName(), PRIMARY)),
            // I18N: Label showing whether the player currently owns an island in admin player info output.
            tr("  - hasIsland: <has-island>", MUTED, unparsed("has-island", String.valueOf(getHasIsland()), PRIMARY)),
            // I18N: Label for the stored home location in admin player info debug output.
            tr("  - home: <home>", MUTED, unparsed("home", LocationUtil.asString(getHomeLocation()), PRIMARY)),
            // I18N: Label for the player's island center location in admin player info debug output.
            tr("  - island: <island>", MUTED, unparsed("island", LocationUtil.asString(getIslandLocation()), PRIMARY)),
            // I18N: Label for the list of island names this player is banned from in admin player info output.
            tr("  - banned from: <banned-islands>", MUTED, unparsed("banned-islands", bannedFrom, PRIMARY)),
            // I18N: Label for the list of islands where this player is trusted in admin player info output.
            tr("  - trusted on: <trusted-islands>", MUTED, unparsed("trusted-islands", trustedOn, PRIMARY))
        };
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    public void banFromIsland(String name) {
        List<String> bannedFrom = playerData.getStringList("bannedFrom");
        if (bannedFrom != null && !bannedFrom.contains(name)) {
            bannedFrom.add(name);
            playerData.set("bannedFrom", bannedFrom);
            save();
        }
    }

    public void unbanFromIsland(String name) {
        List<String> bannedFrom = playerData.getStringList("bannedFrom");
        if (bannedFrom != null && bannedFrom.contains(name)) {
            bannedFrom.remove(name);
            playerData.set("bannedFrom", bannedFrom);
            save();
        }
    }

    @Override
    public List<String> getBannedFrom() {
        return playerData.getStringList("bannedFrom");
    }

    public long getLastSaved() {
        return playerConfigFile.lastModified();
    }

    public void addTrust(String name) {
        List<String> trustedOn = playerData.getStringList("trustedOn");
        if (!trustedOn.contains(name)) {
            trustedOn.add(name);
            playerData.set("trustedOn", trustedOn);
        }
        save();
    }

    public void removeTrust(String name) {
        List<String> trustedOn = playerData.getStringList("trustedOn");
        trustedOn.remove(name);
        playerData.set("trustedOn", trustedOn);
        save();
    }

    @Override
    public List<String> getTrustedOn() {
        return playerData.getStringList("trustedOn");
    }

    public boolean isIslandGenerating() {
        return this.islandGenerating;
    }

    public void setIslandGenerating(boolean value) {
        this.islandGenerating = value;
    }

    @Override
    public IslandInfo getIslandInfo() {
        if (getHasIsland() && locationForParty() != null) {
            return uSkyBlock.getInstance().getIslandInfo(this);
        }
        return null;
    }

    public void setClearInventoryOnNextEntry(boolean b) {
        playerData.set("clearInventoryOnNextEntry", b ? b : null);
        save();
    }

    public boolean isClearInventoryOnNextEntry() {
        return playerData.getBoolean("clearInventoryOnNextEntry", false);
    }

    public void onTeleport(final Player player) {
        if (isClearInventoryOnNextEntry()) {
            scheduler.sync(() -> plugin.clearPlayerInventory(player), TimeUtil.ticksAsDuration(1));
        }
        processPendingBiomePermissionRemovals(player);
        List<String> pending = playerData.getStringList("pending-commands");
        if (!pending.isEmpty()) {
            plugin.execCommands(player, pending);
            playerData.set("pending-commands", null);
            save();
        }
        List<String> pendingPermissions = playerData.getStringList("pending-permissions");
        if (!pendingPermissions.isEmpty()) {
            if (addPermissions(pendingPermissions)) {
                playerData.set("pending-permissions", null);
            }
            save();
        }
    }

    public boolean execCommands(@Nullable List<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return true;
        }
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            uSkyBlock.getInstance().execCommands(player, commands);
            return true;
        } else {
            List<String> pending = playerData.getStringList("pending-commands");
            pending.addAll(commands);
            playerData.set("pending-commands", pending);
            save();
            return false;
        }
    }

    public boolean addPermissions(@Nullable List<String> perms) {
        if (perms == null || perms.isEmpty()) {
            return true;
        }

        List<String> requestedPermissions = perms.stream()
            .filter(s -> s != null && !s.isBlank())
            .map(s -> s.trim().toLowerCase(Locale.ROOT))
            .toList();
        if (requestedPermissions.isEmpty()) {
            return true;
        }

        Set<String> unlockedBiomes = getUnlockedBiomes();
        boolean unlockedBiomesChanged = false;
        List<String> nonBiomePermissions = new ArrayList<>();
        for (String perm : requestedPermissions) {
            String biomeKey = biomeKeyFromPermission(perm);
            if (biomeKey != null) {
                unlockedBiomesChanged = unlockedBiomes.add(biomeKey) || unlockedBiomesChanged;
            } else {
                nonBiomePermissions.add(perm);
            }
        }
        if (unlockedBiomesChanged) {
            setUnlockedBiomes(unlockedBiomes);
        }
        if (nonBiomePermissions.isEmpty()) {
            if (unlockedBiomesChanged) {
                save();
            }
            return true;
        }

        Player target = getPlayer();
        Optional<PermissionsHook> hook = plugin.getHookManager().getPermissionsHook();

        if (target != null && target.isOnline() && hook.isPresent()) {
            List<String> permList = playerData.getStringList(PLAYER_PERMS_PATH);
            PermissionsHook pHook = hook.get();

            for (String perm : nonBiomePermissions) {
                if (!pHook.hasPermission(target, perm)) {
                    permList.add(perm);
                    pHook.addPermission(target, perm);
                }
            }
            playerData.set(PLAYER_PERMS_PATH, permList);
            save();
            return true;
        } else {
            List<String> pending = playerData.getStringList(PENDING_PERMISSIONS_PATH);
            pending.addAll(nonBiomePermissions);
            playerData.set(PENDING_PERMISSIONS_PATH, pending);
            save();
            return false;
        }
    }

    public boolean hasUnlockedBiome(@NotNull String biomeKey) {
        String normalized = biomeKey.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }
        if (Settings.general_defaultUnlockedBiomes.contains(normalized)) {
            return true;
        }
        return getUnlockedBiomes().contains(normalized);
    }

    public void clearUnlockedBiomes() {
        if (playerData.contains(PLAYER_UNLOCKED_BIOMES_PATH)) {
            playerData.set(PLAYER_UNLOCKED_BIOMES_PATH, null);
            save();
        }
    }

    public void clearLegacyBiomePermissions(@NotNull Player target) {
        Validate.notNull(target, "Target cannot be null!");

        Set<String> permissionsToRemove = new LinkedHashSet<>();

        List<String> storedPermissions = new ArrayList<>(playerData.getStringList(PLAYER_PERMS_PATH));
        List<String> nonBiomeStoredPermissions = new ArrayList<>();
        for (String permission : storedPermissions) {
            if (isBiomePermission(permission)) {
                permissionsToRemove.add(normalizePermission(permission));
            } else {
                nonBiomeStoredPermissions.add(permission);
            }
        }
        playerData.set(PLAYER_PERMS_PATH, nonBiomeStoredPermissions.isEmpty() ? null : nonBiomeStoredPermissions);

        List<String> pendingPermissions = new ArrayList<>(playerData.getStringList(PENDING_PERMISSIONS_PATH));
        List<String> nonBiomePendingPermissions = new ArrayList<>();
        for (String permission : pendingPermissions) {
            if (isBiomePermission(permission)) {
                permissionsToRemove.add(normalizePermission(permission));
            } else {
                nonBiomePendingPermissions.add(permission);
            }
        }
        playerData.set(PENDING_PERMISSIONS_PATH, nonBiomePendingPermissions.isEmpty() ? null : nonBiomePendingPermissions);

        permissionsToRemove.addAll(playerData.getStringList(PENDING_BIOME_PERMISSION_REMOVALS_PATH));
        if (!permissionsToRemove.isEmpty()) {
            playerData.set(PENDING_BIOME_PERMISSION_REMOVALS_PATH, List.copyOf(permissionsToRemove));
            save();
            processPendingBiomePermissionRemovals(target);
        } else if (playerData.contains(PENDING_BIOME_PERMISSION_REMOVALS_PATH)) {
            playerData.set(PENDING_BIOME_PERMISSION_REMOVALS_PATH, null);
            save();
        }
    }

    public void clearPerms(@NotNull Player target) {
        Validate.notNull(target, "Target cannot be null!");

        final List<String> perms = playerData.getStringList(PLAYER_PERMS_PATH);
        if (!perms.isEmpty()) {
            plugin.getHookManager().getPermissionsHook().ifPresent((hook) -> {
                for (String perm : perms) {
                    hook.removePermission(target, perm);
                }
            });
        }
        playerData.set(PLAYER_PERMS_PATH, null);
        playerData.set(PENDING_PERMISSIONS_PATH, null);
        save();
    }

    private void migrateLegacyBiomePermissions() {
        Set<String> biomePermissionNodes = new LinkedHashSet<>();
        Set<String> unlockedBiomes = getUnlockedBiomes();
        boolean changed = false;

        List<String> storedPermissions = new ArrayList<>(playerData.getStringList(PLAYER_PERMS_PATH));
        List<String> filteredStoredPermissions = new ArrayList<>();
        for (String permission : storedPermissions) {
            String biomeKey = biomeKeyFromPermission(permission);
            if (biomeKey != null) {
                biomePermissionNodes.add(normalizePermission(permission));
                changed = unlockedBiomes.add(biomeKey) || changed;
            } else {
                filteredStoredPermissions.add(permission);
            }
        }
        if (filteredStoredPermissions.size() != storedPermissions.size()) {
            playerData.set(PLAYER_PERMS_PATH, filteredStoredPermissions.isEmpty() ? null : filteredStoredPermissions);
            changed = true;
        }

        List<String> pendingPermissions = new ArrayList<>(playerData.getStringList(PENDING_PERMISSIONS_PATH));
        List<String> filteredPendingPermissions = new ArrayList<>();
        for (String permission : pendingPermissions) {
            String biomeKey = biomeKeyFromPermission(permission);
            if (biomeKey != null) {
                biomePermissionNodes.add(normalizePermission(permission));
                changed = unlockedBiomes.add(biomeKey) || changed;
            } else {
                filteredPendingPermissions.add(permission);
            }
        }
        if (filteredPendingPermissions.size() != pendingPermissions.size()) {
            playerData.set(PENDING_PERMISSIONS_PATH, filteredPendingPermissions.isEmpty() ? null : filteredPendingPermissions);
            changed = true;
        }

        if (!biomePermissionNodes.isEmpty()) {
            Set<String> pendingRemovals = new LinkedHashSet<>(playerData.getStringList(PENDING_BIOME_PERMISSION_REMOVALS_PATH));
            if (pendingRemovals.addAll(biomePermissionNodes)) {
                playerData.set(PENDING_BIOME_PERMISSION_REMOVALS_PATH, List.copyOf(pendingRemovals));
                changed = true;
            }
        }

        if (changed) {
            setUnlockedBiomes(unlockedBiomes);
            save();
        }
    }

    private void processPendingBiomePermissionRemovals(@Nullable Player target) {
        if (target == null || !target.isOnline()) {
            return;
        }
        List<String> pendingRemovals = playerData.getStringList(PENDING_BIOME_PERMISSION_REMOVALS_PATH);
        if (pendingRemovals.isEmpty()) {
            return;
        }
        plugin.getHookManager().getPermissionsHook().ifPresent(hook -> {
            for (String permission : pendingRemovals) {
                hook.removePermission(target, normalizePermission(permission));
            }
            playerData.set(PENDING_BIOME_PERMISSION_REMOVALS_PATH, null);
            save();
        });
    }

    private @NotNull Set<String> getUnlockedBiomes() {
        Set<String> unlockedBiomes = new LinkedHashSet<>();
        for (String biome : playerData.getStringList(PLAYER_UNLOCKED_BIOMES_PATH)) {
            if (biome == null || biome.isBlank()) {
                continue;
            }
            unlockedBiomes.add(biome.trim().toLowerCase(Locale.ROOT));
        }
        return unlockedBiomes;
    }

    private void setUnlockedBiomes(@NotNull Set<String> unlockedBiomes) {
        playerData.set(PLAYER_UNLOCKED_BIOMES_PATH, unlockedBiomes.isEmpty() ? null : List.copyOf(unlockedBiomes));
    }

    private static boolean isBiomePermission(@Nullable String permission) {
        return biomeKeyFromPermission(permission) != null;
    }

    private static @Nullable String biomeKeyFromPermission(@Nullable String permission) {
        if (permission == null || permission.isBlank()) {
            return null;
        }
        String normalizedPermission = normalizePermission(permission);
        if (!normalizedPermission.startsWith(BIOME_PERMISSION_PREFIX)) {
            return null;
        }
        if (normalizedPermission.length() <= BIOME_PERMISSION_PREFIX.length()) {
            return null;
        }
        return normalizedPermission.substring(BIOME_PERMISSION_PREFIX.length());
    }

    private static @NotNull String normalizePermission(@NotNull String permission) {
        return permission.trim().toLowerCase(Locale.ROOT);
    }
}
