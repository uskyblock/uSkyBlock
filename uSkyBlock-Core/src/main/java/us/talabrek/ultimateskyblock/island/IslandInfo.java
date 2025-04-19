package us.talabrek.ultimateskyblock.island;

import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.apache.commons.lang3.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.event.island.IslandBanPlayerEvent;
import us.talabrek.ultimateskyblock.api.event.island.IslandLockEvent;
import us.talabrek.ultimateskyblock.api.event.island.IslandTrustPlayerEvent;
import us.talabrek.ultimateskyblock.api.event.island.IslandUnbanPlayerEvent;
import us.talabrek.ultimateskyblock.api.event.island.IslandUnlockEvent;
import us.talabrek.ultimateskyblock.api.event.island.IslandUntrustPlayerEvent;
import us.talabrek.ultimateskyblock.api.model.Island;
import us.talabrek.ultimateskyblock.api.model.IslandAccess;
import us.talabrek.ultimateskyblock.api.model.IslandLocation;
import us.talabrek.ultimateskyblock.api.model.IslandLogLine;
import us.talabrek.ultimateskyblock.api.model.IslandPartyMember;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.player.Perk;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Data object for an island
 */
public class IslandInfo implements us.talabrek.ultimateskyblock.api.IslandInfo {
    private final uSkyBlock plugin;
    private final String name;
    private boolean toBeDeleted = false;
    private final UUID islandUuid;

    @Deprecated
    public IslandInfo(@NotNull String islandName, @NotNull uSkyBlock plugin) {
        Validate.notNull(islandName, "IslandName cannot be null");
        Validate.notEmpty(islandName, "IslandName cannot be empty");

        this.plugin = plugin;
        name = islandName;

        UUID fetchedUuid = plugin.getStorage().getIslandByName(islandName).join();
        islandUuid = Objects.requireNonNullElseGet(fetchedUuid, UUID::randomUUID);
    }

    public IslandInfo(@NotNull Island island, @NotNull uSkyBlock plugin) {
        Validate.notNull(island, "Island cannot be null");

        this.plugin = plugin;
        name = island.getName();
        islandUuid = island.getUuid();
    }

    @Deprecated
    public void resetIslandConfig(@NotNull final String leader) {
        Validate.notNull(leader, "Leader cannot be null");
        Validate.notEmpty(leader, "Leader cannot be empty");

        UUID leaderUuid = plugin.getPlayerDB().getUUIDFromName(leader);
        resetIslandConfig(leaderUuid);
    }

    public void resetIslandConfig(@NotNull final UUID leader) {
        getIsland().setLevel(0.0D);
        getIsland().setScoreMultiplier(1.0D);
        getIsland().setScoreOffset(0.0D);

        if (getIsland().getIslandLocations().getLocation(IslandLocation.LocationType.CENTER_WORLD) == null) {
            String[] islandLoc = name.split(",");
            int x = Integer.parseInt(islandLoc[0]);
            int z = Integer.parseInt(islandLoc[1]);

            IslandLocation centerWorld = new IslandLocation(
                IslandLocation.LocationType.CENTER_WORLD,
                plugin.getWorldManager().getWorld().getName(),
                x,
                Settings.island_height,
                z,
                0.0D,
                0.0D
            );

            getIsland().getIslandLocations().addLocation(IslandLocation.LocationType.CENTER_WORLD, centerWorld);
        }

        if(getIsland().getIslandLocations().getLocation(IslandLocation.LocationType.CENTER_NETHER) == null
            && plugin.getWorldManager().getNetherWorld() != null) {
            String[] islandLoc = name.split(",");
            int x = Integer.parseInt(islandLoc[0]);
            int z = Integer.parseInt(islandLoc[1]);

            IslandLocation centerNether = new IslandLocation(
                IslandLocation.LocationType.CENTER_NETHER,
                plugin.getWorldManager().getNetherWorld().getName(),
                x,
                Settings.nether_height,
                z,
                0.0D,
                0.0D
            );

            getIsland().getIslandLocations().addLocation(IslandLocation.LocationType.CENTER_NETHER, centerNether);
        }

        getIsland().setWarpActive(false);
        getIsland().getIslandLocations().removeLocation(IslandLocation.LocationType.WARP);
        getIsland().setLeafBreaks(0);
        getIsland().getIslandParty().getPartyMembers().values().forEach(member -> {
            if (member.getRole() == IslandPartyMember.Role.MEMBER) {
                getIsland().getIslandParty().removePartyMember(member.getUuid());
            }
        });

        getIsland().setBiome(Settings.general_defaultBiome);
        getIsland().setOwner(leader);
        getIsland().setHopperCount(0);
        setupPartyLeader(leader);
        sendMessageToIslandGroup(false, marktr("The island has been created."));
    }

    @Deprecated
    public void setupPartyLeader(@NotNull final String leader) {
        Validate.notNull(leader, "Leader cannot be null");
        Validate.notEmpty(leader, "Leader cannot be empty");

        UUID leaderUuid = plugin.getPlayerDB().getUUIDFromName(leader);
        setupPartyLeader(leaderUuid);
    }

    public void setupPartyLeader(@NotNull final UUID leaderUuid) {
        if (getIsland().getIslandParty().getPartySize() == 0) {
            IslandPartyMember member = new IslandPartyMember(
                leaderUuid,
                IslandPartyMember.Role.LEADER,
                Set.of("island.changeBiome", "island.toggleLock", "island.changeWarp",
                    "island.toggleWarp", "island.inviteOthers", "island.kickOthers", "island.banOthers")
            );
            getIsland().getIslandParty().addPartyMember(leaderUuid, member);
        }

        Player onlinePlayer = plugin.getPlayerDB().getPlayer(leaderUuid);
        // The only time the onlinePlayer will be null is if it is being converted from another skyblock plugin.
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            updatePermissionPerks(onlinePlayer, plugin.getPerkLogic().getPerk(onlinePlayer));
        }

        WorldGuardHandler.updateRegion(this);
        save();
    }

    public void addMember(@NotNull final PlayerInfo playerInfo) {
        Validate.notNull(playerInfo, "PlayerInfo cannot be null");

        playerInfo.setJoinParty(getIslandLocation());
        setupPartyMember(playerInfo);
        plugin.getEventLogic().fireMemberJoinedEvent(this, playerInfo);
    }

    public void setupPartyMember(@NotNull final PlayerInfo member) {
        Validate.notNull(member, "Member cannot be null");

        if (!getIsland().getIslandParty().isMember(member.getUniqueId())) {
            IslandPartyMember partyMember = new IslandPartyMember(
                member.getUniqueId(),
                IslandPartyMember.Role.MEMBER,
                new HashSet<>()
            );
            getIsland().getIslandParty().addPartyMember(member.getUniqueId(), partyMember);
        }

        Player onlinePlayer = member.getPlayer();
        // The only time the onlinePlayer will be null is if it is being converted from another skyblock plugin.
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            updatePermissionPerks(onlinePlayer, plugin.getPerkLogic().getPerk(onlinePlayer));
        }
        WorldGuardHandler.updateRegion(this);
        save();
    }

    public void updatePermissionPerks(@NotNull final Player member, @NotNull Perk perk) {
        Validate.notNull(member, "Member cannot be null");
        Validate.notNull(perk, "Perk cannot be null");

        Integer maxParty = getIsland().getIslandLimits().getPluginLimit("maxPartySizePermission");
        if (maxParty == null || !maxParty.equals(perk.getMaxPartySize())) {
            getIsland().getIslandLimits().setPluginLimit("maxPartySizePermission", perk.getMaxPartySize());
        }

        Integer maxAnimals = getIsland().getIslandLimits().getPluginLimit("maxAnimals");
        if (maxAnimals == null || !maxAnimals.equals(perk.getAnimals())) {
            getIsland().getIslandLimits().setPluginLimit("maxAnimals", perk.getAnimals());
        }

        Integer maxMonsters = getIsland().getIslandLimits().getPluginLimit("maxMonsters");
        if (maxMonsters == null || !maxMonsters.equals(perk.getMonsters())) {
            getIsland().getIslandLimits().setPluginLimit("maxMonsters", perk.getMonsters());
        }

        Integer maxVillagers = getIsland().getIslandLimits().getPluginLimit("maxVillagers");
        if (maxVillagers == null || !maxVillagers.equals(perk.getVillagers())) {
            getIsland().getIslandLimits().setPluginLimit("maxVillagers", perk.getVillagers());
        }

        Integer maxGolems = getIsland().getIslandLimits().getPluginLimit("maxGolems");
        if (maxGolems == null || !maxGolems.equals(perk.getGolems())) {
            getIsland().getIslandLimits().setPluginLimit("maxGolems", perk.getGolems());
        }

        if (!getIsland().getIslandLimits().getBlockLimits().isEmpty()) {
            getIsland().getIslandLimits().getBlockLimits().clear();
            getIsland().getIslandLimits().setBlockLimits(perk.getBlockLimits());
        }
        save();
    }

    public void save() {
        if (toBeDeleted) {
            plugin.getStorage().deleteIsland(getIsland());
        } else {
            plugin.getStorage().saveIsland(getIsland());
        }
    }

    @Override
    public int getMaxPartySize() {
        int defaultMaxPartySize = plugin.getPerkLogic().getIslandPerk(getSchematicName()).getPerk().getMaxPartySize();
        Integer maxPartySizeLimit = getIsland().getIslandLimits().getPluginLimit("maxPartySizePermission");

        return (maxPartySizeLimit != null && maxPartySizeLimit > defaultMaxPartySize) ? maxPartySizeLimit : defaultMaxPartySize;
    }

    @Override
    public int getMaxAnimals() {
        int defaultMaxAnimals = plugin.getPerkLogic().getIslandPerk(getSchematicName()).getPerk().getAnimals();
        Integer maxAnimalsLimit = getIsland().getIslandLimits().getPluginLimit("maxAnimals");

        return (maxAnimalsLimit != null && maxAnimalsLimit > defaultMaxAnimals) ? maxAnimalsLimit : defaultMaxAnimals;
    }

    @Override
    public int getMaxMonsters() {
        int defaultMaxMonsters = plugin.getPerkLogic().getIslandPerk(getSchematicName()).getPerk().getMonsters();
        Integer maxMonstersLimit = getIsland().getIslandLimits().getPluginLimit("maxMonsters");

        return (maxMonstersLimit != null && maxMonstersLimit > defaultMaxMonsters) ? maxMonstersLimit : defaultMaxMonsters;
    }

    @Override
    public int getMaxVillagers() {
        int defaultMaxVillagers = plugin.getPerkLogic().getIslandPerk(getSchematicName()).getPerk().getGolems();
        Integer maxVillagersLimit = getIsland().getIslandLimits().getPluginLimit("maxVillagers");

        return (maxVillagersLimit != null && maxVillagersLimit > defaultMaxVillagers) ? maxVillagersLimit : defaultMaxVillagers;
    }

    @Override
    public int getMaxGolems() {
        int defaultMaxGolems = plugin.getPerkLogic().getIslandPerk(getSchematicName()).getPerk().getGolems();
        Integer maxGolemsLimit = getIsland().getIslandLimits().getPluginLimit("maxGolems");

        return (maxGolemsLimit != null && maxGolemsLimit > defaultMaxGolems) ? maxGolemsLimit : defaultMaxGolems;
    }

    @Override
    @NotNull
    public Map<Material, Integer> getBlockLimits() {
        return getIsland().getIslandLimits().getBlockLimits();
    }

    @Override
    public String getLeader() {
        return plugin.getStorage().getPlayer(getLeaderUniqueId()).join().getName();
    }

    public UUID getLeaderUniqueId() {
        return getIsland().getOwner();
    }

    public boolean hasPerm(Player player, String perm) {
        return hasPerm(player.getUniqueId(), perm);
    }

    public boolean hasPerm(String playerName, String perm) {
        return hasPerm(plugin.getPlayerDB().getUUIDFromName(playerName), perm);
    }

    public boolean hasPerm(UUID uuid, String perm) {
        if (uuid.equals(getLeaderUniqueId())) return true;

        return switch (perm) {
            case "canChangeBiome" -> getIsland().getIslandParty().getPartyMember(uuid).hasPermission("island.canChangeBiome");
            case "canToggleLock" -> getIsland().getIslandParty().getPartyMember(uuid).hasPermission("island.canToggleLock");
            case "canChangeWarp" -> getIsland().getIslandParty().getPartyMember(uuid).hasPermission("island.canChangeWarp");
            case "canToggleWarp" -> getIsland().getIslandParty().getPartyMember(uuid).hasPermission("island.canToggleWarp");
            case "canInviteOthers" -> getIsland().getIslandParty().getPartyMember(uuid).hasPermission("island.canInviteOthers");
            case "canKickOthers" -> getIsland().getIslandParty().getPartyMember(uuid).hasPermission("island.canKickOthers");
            case "canBanOthers" -> getIsland().getIslandParty().getPartyMember(uuid).hasPermission("island.canBanOthers");
            default -> {
                plugin.getLog4JLogger().info("Unknown permission lookup: {}", perm);
                yield false;
            }
        };
    }

    @Override
    public Biome getIslandBiome() {
        return getIsland().getBiome();
    }

    @Override
    public String getBiomeName() {
        return getIslandBiome().getKey().getKey();
    }

    public void setBiome(@NotNull Biome biome) {
        Validate.notNull(biome, "Biome cannot be null");
        getIsland().setBiome(biome);
        save();
    }

    public void setWarpLocation(@Nullable Location loc) {
        if (loc == null) {
            return;
        }

        IslandLocation warpLocation = new IslandLocation(IslandLocation.LocationType.WARP, loc);
        getIsland().getIslandLocations().addLocation(IslandLocation.LocationType.WARP, warpLocation);
        save();
    }

    public boolean togglePerm(@NotNull final String playername, @NotNull final String perm) {
        Validate.notNull(playername, "Playername cannot be null");
        Validate.notEmpty(playername, "Playername cannot be empty");

        UUID uuid = plugin.getPlayerDB().getUUIDFromName(playername);
        return togglePerm(uuid, "island." + perm);
    }

    public boolean togglePerm(@NotNull final UUID playerId, @NotNull final String perm) {
        Validate.notNull(playerId, "Playername cannot be null");
        Validate.notNull(perm, "Perm cannot be null");
        Validate.notEmpty(perm, "Perm cannot be empty");

        if (!getIsland().getIslandParty().isMember(playerId)) {
            plugin.getLog4JLogger().info("Unable to toggle permission for player {}, not a member of island {}.", playerId, getIsland().getName());
            return false;
        }

        IslandPartyMember member = getIsland().getIslandParty().getPartyMembers().get(playerId);

        if (member.hasPermission(perm)) {
            member.removePermission(perm);
        } else {
            member.setPermission(perm);
        }

        return true;
    }

    @Override
    @NotNull
    public Set<String> getMembers() {
        return getMemberUUIDs().stream()
            .map(uuid -> plugin.getPlayerDB().getName(uuid))
            .collect(Collectors.toSet());
    }

    @NotNull
    public Set<UUID> getMemberUUIDs() {
        return getIsland().getIslandParty().getPartyMembers().keySet();
    }

    public boolean isMember(@NotNull OfflinePlayer target) {
        Validate.notNull(target, "Target cannot be null");
        return getIsland().getIslandParty().getPartyMembers().containsKey(target.getUniqueId());
    }

    public void log(@NotNull String message, @Nullable Object[] args) {
        Validate.notNull(message, "Message cannot be null");
        Validate.notEmpty(message, "Message cannot be empty");

        String[] stringArgs = Arrays.stream(args)
            .map(String.class::cast)
            .toArray(String[]::new);
        getIsland().getIslandLog().log(new IslandLogLine(message, stringArgs));
        save();
    }

    @Override
    public int getPartySize() {
        return getIsland().getIslandParty().getPartySize();
    }

    public boolean isLeader(@NotNull OfflinePlayer target) {
        Validate.notNull(target, "Target cannot be null");

        return isLeader(target.getUniqueId());
    }

    @Override
    public boolean isLeader(@NotNull Player player) {
        return getIsland().getIslandParty().isLeader(player.getUniqueId());
    }

    @Deprecated
    public boolean isLeader(String playerName) {
        return isLeader(plugin.getPlayerDB().getUUIDFromName(playerName));
    }

    public boolean isLeader(@NotNull UUID playerId) {
        return getIsland().getIslandParty().isLeader(playerId);
    }

    public boolean hasWarp() {
        return getIsland().isWarpActive();
    }

    public boolean isLocked() {
        return getIsland().isLocked();
    }

    @Override
    public String getName() {
        return getIsland().getName();
    }

    public void setWarp(boolean active) {
        getIsland().setWarpActive(active);
        save();
    }

    /**
     * Locks the island. Might get cancelled via the fired {@link IslandLockEvent}.
     *
     * @param player {@link Player} initializing the lock.
     * @return True if the island was locked, false otherwise, e.g. when the event is cancelled.
     */
    public boolean lock(@NotNull Player player) {
        Validate.notNull(player, "Player cannot be null");

        IslandLockEvent event = new IslandLockEvent(this, player);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        WorldGuardHandler.islandLock(player, name);
        getIsland().setLocked(true);

        sendMessageToIslandGroup(true, marktr("\u00a7b{0}\u00a7d locked the island."), player.getName());
        if (hasWarp()) {
            getIsland().setWarpActive(false);

            player.sendMessage(tr("\u00a74Since your island is locked, your incoming warp has been deactivated."));
            sendMessageToIslandGroup(true, marktr("\u00a7b{0}\u00a7d deactivated the island warp."), player.getName());
        }
        save();
        return true;
    }

    /**
     * Unlocks the island. Might get cancelled via the fired {@link IslandUnlockEvent}.
     *
     * @param player {@link Player} initializing the unlock.
     * @return True if the island was unlocked, false otherwise, e.g. when the event is cancelled.
     */
    public boolean unlock(@NotNull Player player) {
        Validate.notNull(player, "Player cannot be null");

        IslandUnlockEvent event = new IslandUnlockEvent(this, player);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        WorldGuardHandler.islandUnlock(player, name);
        getIsland().setLocked(false);
        sendMessageToIslandGroup(true, marktr("\u00a7b{0}\u00a7d unlocked the island."), player.getName());
        save();
        return true;
    }

    public void sendMessageToIslandGroup(boolean broadcast, @NotNull String message, @Nullable Object... args) {
        Validate.notNull(message, "Message cannot be null");
        Validate.notEmpty(message, "Message cannot be empty");

        if (broadcast) {
            for (UUID uuid : getMemberUUIDs()) {
                Player player = plugin.getPlayerDB().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.sendMessage(tr("\u00a7cSKY \u00a7f> \u00a77 {0}", tr(message, args)));
                }
            }
        }
        log(message, args);
    }

    @Override
    public boolean isBanned(@NotNull OfflinePlayer target) {
        Validate.notNull(target, "Target cannot be null");

        if (target.isOnline()) {
            return isBanned((Player) target);
        }
        return isBanned(target.getUniqueId());
    }

    @Override
    public boolean isBanned(Player player) {
        return isBanned(player.getUniqueId()) && !player.hasPermission("usb.mod.bypassprotection");
    }

    public boolean isBanned(String name) {
        return isBanned(plugin.getPlayerDB().getUUIDFromName(name));
    }

    public boolean isBanned(UUID uuid) {
        return getIsland().getIslandAccessList().isBanned(uuid);
    }

    @Override
    public boolean banPlayer(@NotNull OfflinePlayer target) {
        Validate.notNull(target, "Target cannot be null");

        return trustPlayer(target, null);
    }

    @Override
    public boolean banPlayer(@NotNull OfflinePlayer target, @Nullable OfflinePlayer initializer) {
        Validate.notNull(target, "Target cannot be null");

        if (isBanned(target) || isMember(target) || isLeader(target)) {
            return false;
        }

        IslandBanPlayerEvent event = new IslandBanPlayerEvent(this, target, initializer);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        getIsland().getIslandAccessList().addIslandAccess(new IslandAccess(target.getUniqueId(), IslandAccess.AccessType.BANNED));
        save();
        return true;
    }

    @Override
    public boolean unbanPlayer(@NotNull OfflinePlayer target) {
        Validate.notNull(target, "Target cannot be null");

        return unbanPlayer(target, null);
    }

    @Override
    public boolean unbanPlayer(@NotNull OfflinePlayer target, @Nullable OfflinePlayer initializer) {
        Validate.notNull(target, "Target cannot be null");

        if (!isBanned(target) || isMember(target) || isLeader(target)) {
            return false;
        }

        IslandUnbanPlayerEvent event = new IslandUnbanPlayerEvent(this, target, initializer);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        getIsland().getIslandAccessList().removeIslandAccess(target.getUniqueId());
        save();
        return true;
    }

    @Deprecated
    public void banPlayer(@NotNull UUID uuid) {
        Validate.notNull(uuid, "Uuid cannot be null");

        getIsland().getIslandAccessList().addIslandAccess(
            new IslandAccess(uuid, IslandAccess.AccessType.BANNED));
        save();
    }

    @Override
    @NotNull
    public List<String> getBans() {
        List<String> banned = new ArrayList<>();
        getIsland().getIslandAccessList().getAcl().forEach((uuid, access) -> {
            if (access.getAccessType() == IslandAccess.AccessType.BANNED) {
                String bannedName = plugin.getPlayerDB().getName(uuid);
                if (bannedName != null) {
                    banned.add(bannedName);
                } else {
                    plugin.getLogger().warning("Island " + getIsland().getName() + " has invalid banned value " + uuid);
                }
            }
        });
        return banned;
    }

    @Deprecated
    @Override
    @NotNull
    public List<String> getTrustees() {
        List<String> trusted = new ArrayList<>();
        getIsland().getIslandAccessList().getAcl().forEach((uuid, access) -> {
            if (access.getAccessType() == IslandAccess.AccessType.TRUSTED) {
                String trustedName = plugin.getPlayerDB().getName(uuid);
                if (trustedName != null) {
                    trusted.add(trustedName);
                } else {
                    plugin.getLogger().warning("Island " + getIsland().getName() + " has invalid trust value " + uuid);
                }
            }
        });
        return trusted;
    }

    @NotNull
    public List<UUID> getTrusteeUUIDs() {
        return getIsland().getIslandAccessList().getAcl().entrySet().stream()
            .filter(entry -> entry.getValue().getAccessType() == IslandAccess.AccessType.TRUSTED)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    @Override
    public boolean trustPlayer(@NotNull OfflinePlayer target) {
        Validate.notNull(target, "Target cannot be null");

        return trustPlayer(target, null);
    }

    @Override
    public boolean trustPlayer(@NotNull OfflinePlayer target, @Nullable OfflinePlayer initializer) {
        Validate.notNull(target, "Target cannot be null");

        if (isTrusted(target) || isMember(target) || isLeader(target)) {
            return false;
        }

        IslandTrustPlayerEvent event = new IslandTrustPlayerEvent(this, target, initializer);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        getIsland().getIslandAccessList().addIslandAccess(
            new IslandAccess(target.getUniqueId(), IslandAccess.AccessType.TRUSTED));
        save();
        return true;
    }

    @Override
    public boolean untrustPlayer(@NotNull OfflinePlayer target) {
        Validate.notNull(target, "Target cannot be null");

        return untrustPlayer(target, null);
    }

    @Override
    public boolean untrustPlayer(@NotNull OfflinePlayer target, @Nullable OfflinePlayer initializer) {
        Validate.notNull(target, "Target cannot be null");

        if (!isTrusted(target) || isMember(target) || isLeader(target)) {
            return false;
        }

        IslandUntrustPlayerEvent event = new IslandUntrustPlayerEvent(this, target, initializer);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        getIsland().getIslandAccessList().removeIslandAccess(target.getUniqueId());
        return true;
    }

    @Override
    public boolean isTrusted(@NotNull OfflinePlayer target) {
        Validate.notNull(target, "Target cannot be null");
        return getIsland().getIslandAccessList().isTrusted(target.getUniqueId());
    }

    public void removeMember(@NotNull PlayerInfo member) {
        Validate.notNull(member, "Member cannot be null");

        member.setHomeLocation(null);
        member.removeFromIsland();
        member.save();
        getIsland().getIslandParty().removePartyMember(member.getUniqueId());

        sendMessageToIslandGroup(true, marktr("\u00a7b{0}\u00a7d has been removed from the island group."), member.getPlayerName());
        WorldGuardHandler.updateRegion(this);
        plugin.getEventLogic().fireMemberLeftEvent(this, member);
        save();
    }

    public void setLevel(double score) {
        getIsland().setLevel(score);
        save();
    }

    @Override
    public double getLevel() {
        return getMembers().isEmpty() ? 0 : getIsland().getLevel();
    }

    public void setRegionVersion(String version) {
        getIsland().setRegionVersion(version);
        save();
    }

    public String getRegionVersion() {
        return getIsland().getRegionVersion();
    }

    @Override
    @NotNull
    public List<String> getLog() {
        List<String> convertedList = new ArrayList<>();
        Instant now = Instant.now();

        getIsland().getIslandLog().getLog().forEach(logLine -> {
            if (logLine.getVariables().length > 0) {
                convertedList.add(tr("\u00a79{1} \u00a77- {0}", TimeUtil.durationAsString(Duration.between(logLine.getTimestamp(), now)),
                    tr(logLine.getLine(), (Object[]) logLine.getVariables())));
            } else {
                convertedList.add(tr("\u00a79{1} \u00a77- {0}", TimeUtil.durationAsString(Duration.between(logLine.getTimestamp(), now)),
                    logLine.getLine()));
            }

        });

        return convertedList;
    }

    @Override
    public boolean isParty() {
        return getMembers().size() > 1;
    }

    @Override
    @Nullable
    public Location getWarpLocation() {
        IslandLocation warpLocation = getIsland().getIslandLocations().getLocation(IslandLocation.LocationType.WARP);
        if (hasWarp() && warpLocation != null) {
            return warpLocation.asBukkitLocation();
        }
        return null;
    }

    @Override
    public Location getIslandLocation() {
        return getIsland().getIslandLocations().getLocation(IslandLocation.LocationType.CENTER_WORLD).asBukkitLocation();
    }

    @Override
    public String toString() {
        String str = "\u00a7bIsland Info:\n";
        str += ChatColor.GRAY + "  - level: " + ChatColor.DARK_AQUA + String.format("%5.2f", getLevel()) + "\n";
        str += ChatColor.GRAY + "  - location: " + ChatColor.DARK_AQUA + name + "\n";
        str += ChatColor.GRAY + "  - biome: " + ChatColor.DARK_AQUA + getBiomeName() + "\n";
        str += ChatColor.GRAY + "  - schematic: " + ChatColor.DARK_AQUA + getSchematicName() + "\n";
        str += ChatColor.GRAY + "  - warp: " + ChatColor.DARK_AQUA + hasWarp() + "\n";
        if (hasWarp()) {
            str += ChatColor.GRAY + "     loc: " + ChatColor.DARK_AQUA + LocationUtil.asString(getWarpLocation()) + "\n";
        }
        str += ChatColor.GRAY + "  - locked: " + ChatColor.DARK_AQUA + isLocked() + "\n";
        str += ChatColor.GRAY + "  - ignore: " + ChatColor.DARK_AQUA + ignore() + "\n";
        str += ChatColor.DARK_AQUA + "Party:\n";
        str += ChatColor.GRAY + "  - leader: " + ChatColor.DARK_AQUA + getLeader() + "\n";
        str += ChatColor.GRAY + "  - members: " + ChatColor.DARK_AQUA + getMembers() + "\n";
        str += ChatColor.GRAY + "  - size: " + ChatColor.DARK_AQUA + getPartySize() + "\n";
        str += ChatColor.DARK_AQUA + "Limits:\n";
        str += ChatColor.GRAY + "  - maxParty: " + ChatColor.DARK_AQUA + getMaxPartySize() + "\n";
        str += ChatColor.GRAY + "  - animals: " + ChatColor.DARK_AQUA + getMaxAnimals() + "\n";
        str += ChatColor.GRAY + "  - monsters: " + ChatColor.DARK_AQUA + getMaxMonsters() + "\n";
        str += ChatColor.GRAY + "  - villagers: " + ChatColor.DARK_AQUA + getMaxVillagers() + "\n";
        str += ChatColor.DARK_AQUA + "Bans:\n";
        for (String ban : getBans()) {
            str += ChatColor.GRAY + "  - " + ban + "\n";
        }
        str += ChatColor.DARK_AQUA + "Log:\n";
        for (String log : getLog()) {
            str += ChatColor.GRAY + "  - " + log + "\n";
        }
        return str;
    }

    @Override
    public boolean hasOnlineMembers() {
        return !getOnlineMembers().isEmpty();
    }

    @Override
    public List<Player> getOnlineMembers() {
        return getIsland().getIslandParty().getPartyMembers().keySet().stream()
            .map(plugin.getServer()::getPlayer)
            .filter(player -> player != null && player.isOnline())
            .collect(Collectors.toList());
    }

    @Override
    public boolean contains(Location loc) {
        return name.equalsIgnoreCase(WorldGuardHandler.getIslandNameAt(loc));
    }

    public void sendMessageToOnlineMembers(String msg) {
        String message = tr("\u00a7cSKY \u00a7f> \u00a77 {0}", msg);
        for (Player player : getOnlineMembers()) {
            player.sendMessage(message);
        }
    }

    public void delete() {
        toBeDeleted = true;
    }

    public boolean ignore() {
        return getIsland().isIgnore();
    }

    public void setIgnore(boolean ignore) {
        getIsland().setIgnore(ignore);
        save();
    }

    public int getLeafBreaks() {
        return getIsland().getLeafBreaks();
    }

    public void setLeafBreaks(int breaks) {
        getIsland().setLeafBreaks(breaks);
        save();
    }

    @Override
    public String getSchematicName() {
        return getIsland().getSchematicName();
    }

    public void setSchematicName(String schematicName) {
        getIsland().setSchematicName(schematicName);
        save();
    }

    @Override
    public double getScoreMultiplier() {
        return getIsland().getScoreMultiplier();
    }

    public void setScoreMultiplier(Double d) {
        getIsland().setScoreMultiplier(d);
        save();
    }

    @Override
    public double getScoreOffset() {
        return getIsland().getScoreOffset();
    }

    public void setScoreOffset(Double d) {
        getIsland().setScoreOffset(d);
        save();
    }

    public int getHopperCount() {
        return getIsland().getHopperCount();
    }

    public void setHopperCount(int i) {
        getIsland().setHopperCount(i);
        save();
    }

    private Island getIsland() {
        return plugin.getIslandLogic().getIsland(islandUuid);
    }

    public boolean exists() {
        return getIsland() != null;
    }
}
