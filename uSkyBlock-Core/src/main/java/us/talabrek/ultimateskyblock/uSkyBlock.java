package us.talabrek.ultimateskyblock;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dk.lockfuglsang.minecraft.command.Command;
import dk.lockfuglsang.minecraft.command.CommandManager;
import dk.lockfuglsang.minecraft.file.FileUtil;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import dk.lockfuglsang.minecraft.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.IslandLevel;
import us.talabrek.ultimateskyblock.api.IslandRank;
import us.talabrek.ultimateskyblock.api.UltimateSkyblock;
import us.talabrek.ultimateskyblock.api.UltimateSkyblockProvider;
import us.talabrek.ultimateskyblock.api.async.Callback;
import us.talabrek.ultimateskyblock.api.event.EventLogic;
import us.talabrek.ultimateskyblock.api.event.uSkyBlockEvent;
import us.talabrek.ultimateskyblock.api.event.uSkyBlockScoreChangedEvent;
import us.talabrek.ultimateskyblock.api.impl.UltimateSkyblockApi;
import us.talabrek.ultimateskyblock.api.uSkyBlockAPI;
import us.talabrek.ultimateskyblock.bootstrap.SkyblockApp;
import us.talabrek.ultimateskyblock.bootstrap.SkyblockModule;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.command.AdminCommand;
import us.talabrek.ultimateskyblock.command.admin.SetMaintenanceCommand;
import us.talabrek.ultimateskyblock.handler.ConfirmHandler;
import us.talabrek.ultimateskyblock.handler.CooldownHandler;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.imports.BlockRequirementConverter;
import us.talabrek.ultimateskyblock.imports.ItemComponentConverter;
import us.talabrek.ultimateskyblock.imports.USBImporterExecutor;
import us.talabrek.ultimateskyblock.imports.storage.CompletionImporter;
import us.talabrek.ultimateskyblock.imports.storage.IslandImporter;
import us.talabrek.ultimateskyblock.imports.storage.PlayerImporter;
import us.talabrek.ultimateskyblock.island.BlockLimitLogic;
import us.talabrek.ultimateskyblock.island.IslandGenerator;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandLocatorLogic;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.island.OrphanLogic;
import us.talabrek.ultimateskyblock.island.level.IslandScore;
import us.talabrek.ultimateskyblock.island.level.LevelLogic;
import us.talabrek.ultimateskyblock.island.task.CreateIslandTask;
import us.talabrek.ultimateskyblock.island.task.SetBiomeTask;
import us.talabrek.ultimateskyblock.menu.SkyBlockMenu;
import us.talabrek.ultimateskyblock.player.IslandPerk;
import us.talabrek.ultimateskyblock.player.PerkLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.player.PlayerNotifier;
import us.talabrek.ultimateskyblock.player.PlayerPerk;
import us.talabrek.ultimateskyblock.player.TeleportLogic;
import us.talabrek.ultimateskyblock.storage.SkyStorage;
import us.talabrek.ultimateskyblock.util.IslandUtil;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.util.ServerUtil;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.time.Duration;

import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static java.util.Objects.requireNonNull;
import static us.talabrek.ultimateskyblock.util.LogUtil.log;

public class uSkyBlock extends JavaPlugin implements uSkyBlockAPI, CommandManager.RequirementChecker {
    private static final String CN = uSkyBlock.class.getName();
    public static final String[][] depends = new String[][]{
        new String[]{"Vault", "1.7.1", "optional"},
        new String[]{"WorldEdit", "7.2.12", "optionalIf", "FastAsyncWorldEdit"},
        new String[]{"WorldGuard", "7.0.8"},
        new String[]{"FastAsyncWorldEdit", "2.4.3", "optional"},
        new String[]{"Multiverse-Core", "4.3.1", "optional"},
        new String[]{"Multiverse-Portals", "4.2.1", "optional"},
        new String[]{"Multiverse-NetherPortals", "4.2.1", "optional"},
    };
    private static String missingRequirements = null;
    private static final Random RND = new Random();

    private SkyblockApp skyBlock;

    // TODO: eventually get rid of these global references and move them to a proper API instead
    @Inject
    private SkyBlockMenu menu;
    @Inject
    private ChallengeLogic challengeLogic;
    @Inject
    private EventLogic eventLogic;
    @Inject
    private LevelLogic levelLogic;
    @Inject
    private IslandLogic islandLogic;
    @Inject
    private OrphanLogic orphanLogic;
    @Inject
    private PerkLogic perkLogic;
    @Inject
    private TeleportLogic teleportLogic;
    @Inject
    private LimitLogic limitLogic;
    @Inject
    private HookManager hookManager;
    @Inject
    private WorldManager worldManager;
    @Inject
    private IslandGenerator islandGenerator;
    @Inject
    private PlayerNotifier notifier;
    @Inject
    private USBImporterExecutor importer;

    @Inject
    private IslandLocatorLogic islandLocatorLogic;
    @Inject
    private PlayerDB playerDB;
    @Inject
    private ConfirmHandler confirmHandler;
    @Inject
    private CooldownHandler cooldownHandler;
    @Inject
    private PlayerLogic playerLogic;
    // TODO: don't assign value directly, but use injection instead. Currently, legacy code in PlaceholderHandler accesses it too early for injection to work.
    @Inject
    private PluginConfig config = new PluginConfig();
    @Inject
    private BlockLimitLogic blockLimitLogic;
    @Inject
    private SkyUpdateChecker updateChecker;
    @Inject
    private Scheduler scheduler;

    private UltimateSkyblockApi api;

    // TODO: Move towards injection too.
    private SkyStorage storage;

    private volatile boolean maintenanceMode = false;

    private static uSkyBlock instance;

    @Override
    public void onDisable() {
        deregisterApi(api);
        api = null;
        shutdown();
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        return config.getYamlConfig();
    }

    private void convertConfigItemsTo1_20_6IfRequired() {
        var converter = new ItemComponentConverter(getLogger());
        converter.checkAndDoImport(getDataFolder());
    }

    private void convertConfigToBlockRequirements() {
        var converter = new BlockRequirementConverter(getLogger());
        converter.checkAndDoImport(getDataFolder());
    }

    private void convertFileStorageToSQL() {
        if (Files.exists(getDataFolder().toPath().resolve("uuid2name.yml")) || Files.exists(getDataFolder().toPath().resolve("players"))) {
            getLogger().info("Importing old uuid2name.yml...");
            new PlayerImporter(this);
        }

        if (Files.exists(getDataFolder().toPath().resolve("islands"))) {
            getLogger().info("Importing old islands...");
            new IslandImporter(this);
        }

        if (Files.exists(getDataFolder().toPath().resolve("completion"))) {
            getLogger().info("Importing old completions...");
            new CompletionImporter(this);
        }
    }

    @Override
    public void onEnable() {
        missingRequirements = null;
        instance = this;

        // Converter has to run before the plugin loads its config files.
        convertConfigItemsTo1_20_6IfRequired();
        convertConfigToBlockRequirements();

        // TODO: Move towards startup.
        try {
            storage = new SkyStorage(this);
        } catch (RuntimeException ex) {
            getLogger().severe("Failed to connect to provided database. Shutting down plugin...");
            ex.printStackTrace();
            return;
        }

        convertFileStorageToSQL();

        reloadLegacyStuff();
        startup();

        api = new UltimateSkyblockApi(this);
        registerApi(api);

        getScheduler().sync(() -> {
            ServerUtil.init(uSkyBlock.this);
            if (!isRequirementsMet(Bukkit.getConsoleSender(), null)) {
                return;
            }

            delayedEnable();

            getServer().dispatchCommand(getServer().getConsoleSender(), "usb flush"); // See uskyblock#4
        }, TimeUtil.ticksAsDuration(getConfig().getLong("init.initDelay", 50L)));

        getScheduler().async(() -> getUpdateChecker().checkForUpdates(), Duration.ZERO, Duration.ofHours(4));
    }

    public synchronized boolean isRequirementsMet(CommandSender sender, Command command, String... args) {
        if (maintenanceMode && !(
            (command instanceof AdminCommand && args != null && args.length > 0 && args[0].equals("maintenance")) ||
                command instanceof SetMaintenanceCommand)) {
            sender.sendMessage(tr("\u00a7cMAINTENANCE:\u00a7e uSkyBlock is currently in maintenance mode"));
            return false;
        }
        if (missingRequirements == null) {
            PluginManager pluginManager = getServer().getPluginManager();
            missingRequirements = "";
            for (String[] pluginReq : depends) {
                if (pluginReq.length > 2 && pluginReq[2].equals("optional")) {
                    // Do check the version if an optional requirement is present.
                    if (!pluginManager.isPluginEnabled(pluginReq[0])) {
                        continue;
                    }
                }
                if (pluginReq.length > 2 && pluginReq[2].equals("optionalIf")) {
                    if (pluginManager.isPluginEnabled(pluginReq[3])) {
                        continue;
                    }
                }
                if (pluginManager.isPluginEnabled(pluginReq[0])) {
                    PluginDescriptionFile desc = requireNonNull(pluginManager.getPlugin(pluginReq[0])).getDescription();
                    if (VersionUtil.getVersion(desc.getVersion()).isLT(pluginReq[1])) {
                        missingRequirements += tr("\u00a7buSkyBlock\u00a7e depends on \u00a79{0}\u00a7e >= \u00a7av{1}\u00a7e but only \u00a7cv{2}\u00a7e was found!\n", pluginReq[0], pluginReq[1], desc.getVersion());
                    }
                } else {
                    missingRequirements += tr("\u00a7buSkyBlock\u00a7e depends on \u00a79{0}\u00a7e >= \u00a7av{1}", pluginReq[0], pluginReq[1]);
                }
            }
        }
        if (missingRequirements.isEmpty()) {
            return true;
        } else {
            sender.sendMessage(missingRequirements.split("\n"));
            return false;
        }
    }

    private void createDataFolder() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
    }

    public static uSkyBlock getInstance() {
        return uSkyBlock.instance;
    }

    public Location getSafeHomeLocation(final PlayerInfo p) {
        Location home = LocationUtil.findNearestSafeLocation(p.getHomeLocation(), null);
        if (home == null) {
            home = LocationUtil.findNearestSafeLocation(p.getIslandLocation(), null);
        }
        return home;
    }

    public Location getSafeWarpLocation(final PlayerInfo p) {
        us.talabrek.ultimateskyblock.api.IslandInfo islandInfo = getIslandInfo(p);
        if (islandInfo != null) {
            Location warp = LocationUtil.findNearestSafeLocation(islandInfo.getWarpLocation(), null);
            if (warp == null) {
                warp = LocationUtil.findNearestSafeLocation(islandInfo.getIslandLocation(), null);
            }
            return warp;
        }
        return null;
    }

    private void postDelete(final PlayerInfo pi) {
        pi.save();
    }

    private void postDelete(final IslandInfo islandInfo) {
        WorldGuardHandler.removeIslandRegion(islandInfo.getName());
        islandLogic.deleteIslandConfig(islandInfo.getName());
        orphanLogic.save();
    }

    public boolean deleteEmptyIsland(String islandName, final Runnable runner) {
        final IslandInfo islandInfo = getIslandInfo(islandName);
        if (islandInfo != null && islandInfo.getMembers().isEmpty()) {
            islandLogic.clearIsland(islandInfo.getIslandLocation(), () -> {
                postDelete(islandInfo);
                if (runner != null) {
                    runner.run();
                }
            });
            return true;
        } else {
            return false;
        }
    }

    public void deletePlayerIsland(final String player, final Runnable runner) {
        PlayerInfo pi = playerLogic.getPlayerInfo(player);
        final PlayerInfo finalPI = pi;
        final IslandInfo islandInfo = getIslandInfo(pi);
        Location islandLocation = islandInfo.getIslandLocation();
        for (String member : islandInfo.getMembers()) {
            pi = playerLogic.getPlayerInfo(member);
            islandInfo.removeMember(pi);
        }
        islandLogic.clearIsland(islandLocation, () -> {
            postDelete(finalPI);
            postDelete(islandInfo);
            if (runner != null) runner.run();
        });
    }

    public boolean restartPlayerIsland(final Player player, final Location next, final String cSchem) {
        if (!perkLogic.getSchemes(player).contains(cSchem)) {
            player.sendMessage(tr("\u00a7eYou do not have access to that island-schematic!"));
            return false;
        }
        final PlayerInfo playerInfo = getPlayerInfo(player);
        if (playerInfo != null) {
            playerInfo.setIslandGenerating(true);
        }
        if (getWorldManager().isSkyWorld(player.getWorld())) {
            // Clear first, since the player could log out and we NEED to make sure their inventory gets cleared.
            clearPlayerInventory(player);
        }
        islandLogic.clearIsland(next, () -> generateIsland(player, playerInfo, next, cSchem));
        return true;
    }

    public void clearPlayerInventory(Player player) {
        getLogger().entering(CN, "clearPlayerInventory", player);
        PlayerInfo playerInfo = getPlayerInfo(player);
        if (!getWorldManager().isSkyWorld(player.getWorld())) {
            getLogger().finer("not clearing, since player is not in skyworld, marking for clear on next entry");
            if (playerInfo != null) {
                playerInfo.setClearInventoryOnNextEntry(true);
            }
            return;
        }
        if (playerInfo != null) {
            playerInfo.setClearInventoryOnNextEntry(false);
        }
        if (getConfig().getBoolean("options.restart.clearInventory", true)) {
            player.getInventory().clear();
        }
        if (getConfig().getBoolean("options.restart.clearPerms", true)) {
            playerInfo.clearPerms(player);
        }
        if (getConfig().getBoolean("options.restart.clearArmor", true)) {
            ItemStack[] armor = player.getEquipment().getArmorContents();
            player.getEquipment().setArmorContents(new ItemStack[armor.length]);
        }
        if (getConfig().getBoolean("options.restart.clearEnderChest", true)) {
            player.getEnderChest().clear();
        }
        if (getConfig().getBoolean("options.restart.clearCurrency", false)) {
            getHookManager().getEconomyHook().ifPresent((hook) -> hook.withdrawPlayer(player, hook.getBalance(player)));
        }
        getLogger().exiting(CN, "clearPlayerInventory");
    }

    public synchronized boolean devSetPlayerIsland(final Player sender, final Location l, final String player) {
        final PlayerInfo pi = playerLogic.getPlayerInfo(player);

        String islandName = WorldGuardHandler.getIslandNameAt(l);
        Location islandLocation = IslandUtil.getIslandLocation(islandName);
        final Location newLoc = LocationUtil.alignToDistance(islandLocation, Settings.island_distance);
        if (newLoc == null) {
            return false;
        }

        boolean deleteOldIsland = false;
        if (pi.getHasIsland()) {
            Location oldLoc = pi.getIslandLocation();
            if (oldLoc != null
                && !(newLoc.getBlockX() == oldLoc.getBlockX() && newLoc.getBlockZ() == oldLoc.getBlockZ())) {
                deleteOldIsland = true;
            }
        }

        if (newLoc.equals(pi.getIslandLocation())) {
            sender.sendMessage(tr("\u00a74Player is already assigned to this island!"));
            deleteOldIsland = false;
        }

        // Purge current islandinfo and partymembers if there's an active party at this location (issue #948)
        getIslandLogic().purge(islandName);

        Runnable resetIsland = () -> {
            pi.setHomeLocation(null);
            pi.setHomeLocation(getSafeHomeLocation(pi));
            IslandInfo island = islandLogic.createIslandInfo(pi.locationForParty(), player);
            WorldGuardHandler.updateRegion(island);
            pi.save();
        };
        if (deleteOldIsland) {
            deletePlayerIsland(pi.getPlayerName(), resetIsland);
        } else {
            resetIsland.run();
        }
        return true;
    }

    public boolean playerIsOnIsland(final Player player) {
        return playerIsOnOwnIsland(player)
            || playerIsTrusted(player);
    }

    public boolean playerIsOnOwnIsland(Player player) {
        return locationIsOnIsland(player, player.getLocation())
            || locationIsOnNetherIsland(player, player.getLocation());
    }

    private boolean playerIsTrusted(Player player) {
        String islandName = WorldGuardHandler.getIslandNameAt(player.getLocation());
        UUID islandAt = this.getStorage().getIslandByName(islandName).join();
        if (islandAt != null) {
            us.talabrek.ultimateskyblock.api.IslandInfo islandInfo = islandLogic.getIslandInfo(islandAt);
            return islandInfo != null && islandInfo.isTrusted(player);
        }
        return false;
    }

    public boolean locationIsOnNetherIsland(final Player player, final Location loc) {
        if (!getWorldManager().isSkyNether(loc.getWorld())) {
            return false;
        }
        PlayerInfo playerInfo = playerLogic.getPlayerInfo(player);
        if (playerInfo != null && playerInfo.getHasIsland()) {
            Location p = playerInfo.getIslandNetherLocation();
            if (p == null) {
                return false;
            }
            ProtectedRegion region = WorldGuardHandler.getNetherRegionAt(p);
            return region != null && region.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
        return false;
    }

    public boolean locationIsOnIsland(final Player player, final Location loc) {
        if (!getWorldManager().isSkyWorld(loc.getWorld())) {
            return false;
        }
        PlayerInfo playerInfo = playerLogic.getPlayerInfo(player);
        if (playerInfo != null && playerInfo.getHasIsland()) {
            Location p = playerInfo.getIslandLocation();
            if (p == null) {
                return false;
            }
            ProtectedRegion region = WorldGuardHandler.getIslandRegionAt(p);
            return region != null && region.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
        return false;
    }

    public boolean hasIsland(final Player player) {
        PlayerInfo playerInfo = getPlayerInfo(player);
        return playerInfo != null && playerInfo.getHasIsland();
    }

    public boolean islandAtLocation(final Location loc) {
        return ((!WorldGuardHandler.getIntersectingRegions(loc).isEmpty()) || islandLogic.hasIsland(loc));
    }

    public boolean islandInSpawn(final Location loc) {
        if (loc == null) {
            return true;
        }
        return WorldGuardHandler.isIslandIntersectingSpawn(loc);
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return getWorldManager().getDefaultWorldGenerator(worldName, id);
    }

    public PlayerInfo getPlayerInfo(Player player) {
        return playerLogic.getPlayerInfo(player);
    }

    public PlayerInfo getPlayerInfo(UUID uuid) {
        return playerLogic.getPlayerInfo(uuid);
    }

    public PlayerInfo getPlayerInfo(String playerName) {
        return playerLogic.getPlayerInfo(playerName);
    }

    // TODO: move these to Biome/World related classes
    public @Nullable Biome getBiome(String biomeName) {
        if (biomeName == null) return null;
        return Registry.BIOME.match(biomeName);
    }

    public void setBiome(Location loc, Biome biome) {
        new SetBiomeTask(this, loc, biome, null).runTask(this);
    }

    public void createIsland(final Player player, String cSchem) {
        PlayerInfo pi = getPlayerInfo(player);
        if (pi.isIslandGenerating()) {
            player.sendMessage(tr("\u00a7cYour island is in the process of generating, you cannot create now."));
            return;
        }
        if (!perkLogic.getSchemes(player).contains(cSchem)) {
            player.sendMessage(tr("\u00a7eYou do not have access to that island-schematic!"));
            return;
        }
        pi.setIslandGenerating(true);
        try {
            Location next = getIslandLocatorLogic().getNextIslandLocation(player);
            if (getWorldManager().isSkyWorld(player.getWorld())) {
                getTeleportLogic().spawnTeleport(player, true);
            }
            generateIsland(player, pi, next, cSchem);
        } catch (Exception ex) {
            player.sendMessage(tr("Could not create your Island. Please contact a server moderator."));
            log(Level.SEVERE, "Error creating island", ex);
        }
        log(Level.INFO, "Finished creating player island.");
    }

    private void generateIsland(final Player player, final PlayerInfo pi, final Location next, final String cSchem) {
        if (!perkLogic.getSchemes(player).contains(cSchem)) {
            player.sendMessage(tr("\u00a7eYou do not have access to that island-schematic!"));
            orphanLogic.addOrphan(next);
            return;
        }
        final PlayerPerk playerPerk = new PlayerPerk(pi, perkLogic.getPerk(player));
        player.sendMessage(tr("\u00a7eGetting your island ready, please be patient, it can take a while."));
        BukkitRunnable createTask = new CreateIslandTask(this, player, playerPerk, next, cSchem);
        IslandInfo tempInfo = islandLogic.createIslandInfo(LocationUtil.getIslandName(next), pi.getPlayerName());
        WorldGuardHandler.protectIsland(this, player, tempInfo);
        islandLogic.clearIsland(next, createTask);
    }

    public IslandInfo setNewPlayerIsland(final PlayerInfo playerInfo, final Location loc) {
        playerInfo.startNewIsland(loc);

        Location chestLocation = LocationUtil.findChestLocation(loc);
        Optional<Location> chestSpawnLocation = LocationUtil.findNearestSpawnLocation(
            chestLocation != null ? chestLocation : loc);

        if (chestSpawnLocation.isPresent()) {
            playerInfo.setHomeLocation(chestSpawnLocation.get());
        } else {
            log(Level.SEVERE, "Could not find a safe chest within 15 blocks of the island spawn. Bad schematic!");
        }
        IslandInfo info = islandLogic.createIslandInfo(playerInfo.locationForParty(), playerInfo.getPlayerName());
        Player onlinePlayer = playerInfo.getPlayer();
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            info.updatePermissionPerks(onlinePlayer, perkLogic.getPerk(onlinePlayer));
        }
        if (challengeLogic.isResetOnCreate()) {
            playerInfo.resetAllChallenges();
        }
        playerInfo.save();
        return info;
    }

    public IslandInfo getIslandInfo(Player player) {
        PlayerInfo playerInfo = getPlayerInfo(player);
        return islandLogic.getIslandInfo(playerInfo);
    }

    @Override
    public IslandInfo getIslandInfo(Location location) {
        return getIslandInfo(WorldGuardHandler.getIslandNameAt(location));
    }

    @Override
    public boolean isGTE(String versionNumber) {
        return VersionUtil.getVersion(getDescription().getVersion()).isGTE(versionNumber);
    }

    public IslandInfo getIslandInfo(String location) {
        return islandLogic.getIslandInfo(location);
    }

    public IslandInfo getIslandInfo(PlayerInfo pi) {
        return islandLogic.getIslandInfo(pi);
    }

    public SkyBlockMenu getMenu() {
        return menu;
    }

    public ChallengeLogic getChallengeLogic() {
        return challengeLogic;
    }

    public LevelLogic getLevelLogic() {
        return levelLogic;
    }

    public PerkLogic getPerkLogic() {
        return perkLogic;
    }

    public IslandLocatorLogic getIslandLocatorLogic() {
        return islandLocatorLogic;
    }

    @Override
    public void reloadConfig() {
        reload();
    }

    private void reload() {
        shutdown();
        reloadLegacyStuff();
        startup();
        delayedEnable();
    }

    private void shutdown() {
        if (this.skyBlock != null) {
            this.skyBlock.shutdown(this);
            // TODO: Move towards services.
            storage.destruct();
            this.skyBlock = null;
        }
        WorldManager.skyBlockWorld = null; // Force a reload on config.
    }

    private void startup() {
        if (this.skyBlock != null) {
            throw new IllegalStateException("Skyblock already started");
        }

        WorldManager.skyBlockWorld = null; // Force a re-import or what-ever...
        WorldManager.skyBlockNetherWorld = null;

        Injector injector = Guice.createInjector(new SkyblockModule(this));
        this.skyBlock = injector.getInstance(SkyblockApp.class);
        injector.injectMembers(this);
        this.skyBlock.startup(this);
    }

    private void delayedEnable() {
        this.skyBlock.delayedEnable(this);
    }

    /**
     * Initializes/reloads the legacy static services. This has to be done before the object-oriented services are
     * created, as some use the static services in their constructors. This should be refactored and integrated with
     * the new system in the future.
     */
    private void reloadLegacyStuff() {
        createDataFolder();
        CommandManager.registerRequirements(this);
        FileUtil.setDataFolder(getDataFolder());
        FileUtil.setAlwaysOverwrite("levelConfig.yml");
        Settings.loadPluginConfig(new PluginConfig().getYamlConfig());
        I18nUtil.initialize(getDataFolder(), Settings.locale);
        saveConfig();
        // Update all of the loaded configs.
        FileUtil.reload();
    }

    public IslandLogic getIslandLogic() {
        return islandLogic;
    }

    public OrphanLogic getOrphanLogic() {
        return orphanLogic;
    }

    public BlockLimitLogic getBlockLimitLogic() {
        return blockLimitLogic;
    }

    /**
     * @param player    The player executing the command
     * @param command   The command to execute
     * @param onlyInSky Whether the command is restricted to a sky-associated world.
     */
    public void execCommand(Player player, String command, boolean onlyInSky) {
        if (command == null || player == null) {
            return;
        }
        if (onlyInSky && !getWorldManager().isSkyAssociatedWorld(player.getWorld())) {
            return;
        }
        command = command
            .replaceAll("\\{player\\}", Matcher.quoteReplacement(player.getName()))
            .replaceAll("\\{playerName\\}", Matcher.quoteReplacement(player.getDisplayName()))
            .replaceAll("\\{playername\\}", Matcher.quoteReplacement(player.getDisplayName()))
            .replaceAll("\\{position\\}", Matcher.quoteReplacement(LocationUtil.asString(player.getLocation()))); // Figure out what this should be
        Matcher m = Pattern.compile("^\\{p=(?<prob>0?\\.[0-9]+)\\}(.*)$").matcher(command);
        if (m.matches()) {
            double p = Double.parseDouble(m.group("prob"));
            command = m.group(2);
            if (RND.nextDouble() > p) {
                return; // Skip the command
            }
        }
        m = Pattern.compile("^\\{d=(?<delay>[0-9]+)\\}(.*)$").matcher(command);
        Duration delay = Duration.ZERO;
        if (m.matches()) {
            delay = Duration.ofMillis(Long.parseLong(m.group("delay")));
            command = m.group(2);
        }
        if (command.contains("{party}")) {
            PlayerInfo playerInfo = getPlayerInfo(player);
            IslandInfo islandInfo = getIslandInfo(playerInfo);
            for (String member : islandInfo.getMembers()) {
                doExecCommand(player, command.replaceAll("\\{party\\}", Matcher.quoteReplacement(member)), delay);
            }
        } else {
            doExecCommand(player, command, delay);
        }
    }

    private void doExecCommand(final Player player, final String command, Duration delay) {
        if (delay.isZero()) {
            scheduler.sync(() -> doExecCommand(player, command));
        } else if (delay.isPositive()) {
            scheduler.sync(() -> doExecCommand(player, command), delay);
        } else {
            log(Level.INFO, "WARN: Misconfigured command found, with negative delay! " + command);
        }
    }

    private void doExecCommand(Player player, String command) {
        if (command.startsWith("op:")) {
            if (player.isOp()) {
                player.performCommand(command.substring(3).trim());
            } else {
                player.setOp(true);
                // Prevent privilege escalation if called command throws unhandled exception
                try {
                    player.performCommand(command.substring(3).trim());
                } finally {
                    player.setOp(false);
                }
            }
        } else if (command.startsWith("console:")) {
            getServer().dispatchCommand(getServer().getConsoleSender(), command.substring(8).trim());
        } else {
            player.performCommand(command);
        }
    }

    public USBImporterExecutor getImporter() {
        if (importer == null) {
            importer = new USBImporterExecutor(this);
        }
        return importer;
    }

    public boolean playerIsInSpawn(Player player) {
        Location pLoc = player.getLocation();
        if (!getWorldManager().isSkyWorld(pLoc.getWorld())) {
            return false;
        }
        Location spawnCenter = new Location(WorldManager.skyBlockWorld, 0, pLoc.getBlockY(), 0);
        return spawnCenter.distance(pLoc) <= Settings.general_spawnSize;
    }

    /**
     * Notify the player, but max. every X seconds.
     */
    public void notifyPlayer(Player player, String msg) {
        notifier.notifyPlayer(player, msg);
    }

    public static uSkyBlockAPI getAPI() {
        return getInstance();
    }

    // API

    @Override
    public List<IslandLevel> getTopTen() {
        return getRanks(0, 10);
    }

    @Override
    public List<IslandLevel> getRanks(int offset, int length) {
        return islandLogic != null ? islandLogic.getRanks(offset, length) : Collections.emptyList();
    }

    @Override
    public double getIslandLevel(Player player) {
        PlayerInfo info = getPlayerInfo(player);
        if (info != null) {
            us.talabrek.ultimateskyblock.api.IslandInfo islandInfo = getIslandInfo(info);
            if (islandInfo != null) {
                return islandInfo.getLevel();
            }
        }
        return 0;
    }

    @Override
    public IslandRank getIslandRank(Player player) {
        PlayerInfo playerInfo = getPlayerInfo(player);
        return islandLogic != null && playerInfo != null && playerInfo.getHasIsland() ?
            islandLogic.getRank(playerInfo.locationForParty())
            : null;
    }

    @Override
    public IslandRank getIslandRank(Location location) {
        String islandNameAt = WorldGuardHandler.getIslandNameAt(location);
        if (islandNameAt != null && islandLogic != null) {
            return islandLogic.getRank(islandNameAt);
        }
        return null;
    }

    public void fireChangeEvent(CommandSender sender, uSkyBlockEvent.Cause cause) {
        Player player = (sender instanceof Player) ? (Player) sender : null;
        final uSkyBlockEvent event = new uSkyBlockEvent(player, this, cause);
        fireAsyncEvent(event);
    }

    public void fireAsyncEvent(final Event event) {
        getServer().getScheduler().runTaskAsynchronously(this,
            () -> getServer().getPluginManager().callEvent(event)
        );
    }

    public PlayerDB getPlayerDB() {
        return playerDB;
    }

    private IslandScore adjustScore(IslandScore score, IslandInfo islandInfo) {
        IslandPerk islandPerk = perkLogic.getIslandPerk(islandInfo.getSchematicName());
        double blockScore = score.getScore();
        blockScore = blockScore * islandPerk.getScoreMultiply() * islandInfo.getScoreMultiplier() + islandPerk.getScoreOffset() + islandInfo.getScoreOffset();
        return new IslandScore(blockScore, score.getTop());
    }

    public void calculateScoreAsync(final Player player, String islandName, final Callback<us.talabrek.ultimateskyblock.api.model.IslandScore> callback) {
        final IslandInfo islandInfo = getIslandInfo(islandName);
        getLevelLogic().calculateScoreAsync(islandInfo.getIslandLocation(), new Callback<>() {
            @Override
            public void run() {
                IslandScore score = adjustScore(getState(), islandInfo);
                callback.setState(score);
                islandInfo.setLevel(score.getScore());
                getIslandLogic().updateRank(islandInfo, score);
                fireAsyncEvent(new uSkyBlockScoreChangedEvent(player, getInstance(), score, islandInfo.getIslandLocation()));
                callback.run();
            }
        });
    }

    public ConfirmHandler getConfirmHandler() {
        return confirmHandler;
    }

    public CooldownHandler getCooldownHandler() {
        return cooldownHandler;
    }

    public EventLogic getEventLogic() {
        return eventLogic;
    }

    public PlayerLogic getPlayerLogic() {
        return playerLogic;
    }

    public TeleportLogic getTeleportLogic() {
        return teleportLogic;
    }

    public LimitLogic getLimitLogic() {
        return limitLogic;
    }

    public IslandGenerator getIslandGenerator() {
        return islandGenerator;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public SkyUpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    // TODO: minoneer 06.02.2025: Do we need this? It is currently very buggy and makes a lot of the logic more complex.

    /**
     * CAUTION! If anyone calls this with true, they MUST ensure it is later called with false,
     * or the plugin will effectively be in a locked state.
     *
     * @param maintenanceMode whether or not to enable maintenance-mode.
     */
    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
        if (maintenanceMode) {
            if (playerLogic != null) {
                playerLogic.flushCache();
            }
            if (islandLogic != null) {
                islandLogic.flushCache();
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, String[] args) {
        if (!isRequirementsMet(sender, null, args)) {
            sender.sendMessage(tr("\u00a7cCommand is currently disabled!"));
        }
        return true;
    }

    public void execCommands(Player player, List<String> cmdList) {
        for (String cmd : cmdList) {
            execCommand(player, cmd, false);
        }
    }

    public SkyStorage getStorage() {
        return storage;
    }

    /**
     * Register this uSkyBlock instance with our API provider and Bukkit's ServicesManager.
     */
    private void registerApi(UltimateSkyblock api) {
        UltimateSkyblockProvider.registerPlugin(api);
        getServer().getServicesManager().register(UltimateSkyblock.class, api, this, ServicePriority.Normal);
    }

    /**
     * Deregister this uSkyBlock instance with our API provider and Bukkit's ServicesManager.
     */
    private void deregisterApi(UltimateSkyblock api) {
        UltimateSkyblockProvider.deregisterPlugin();
        getServer().getServicesManager().unregister(api);
    }

    public PluginConfig getPluginConfig() {
        return config;
    }

    public Scheduler getScheduler() {
        return new Scheduler(this);
    }

    public org.slf4j.Logger getLog4JLogger() {
        return org.slf4j.LoggerFactory.getLogger(getLogger().getName());
    }
}
