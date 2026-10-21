package us.talabrek.ultimateskyblock.ittest;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.Challenge;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletion;
import us.talabrek.ultimateskyblock.challenge.ChallengeKey;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.ittest.result.AtomicVerdictWriter;
import us.talabrek.ultimateskyblock.ittest.result.Verdict;
import us.talabrek.ultimateskyblock.ittest.result.VerdictCodec;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class IntegrationTestPlugin extends JavaPlugin implements Listener {
    static final String PLAYER_NAME = "UsbItPlayer";
    static final UUID PLAYER_UUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + PLAYER_NAME).getBytes(StandardCharsets.UTF_8));
    /** The second presence client: the player the fixture player bans from, or locks out of, their island. */
    static final String VISITOR_NAME = "UsbItVisitor";
    static final String SCHEME_ID = "ittest";
    static final ChallengeKey CHALLENGE_ID = ChallengeKey.of("ittest_trade");
    private static final Duration SETUP_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration PLAYER_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration ISLAND_TIMEOUT = Duration.ofSeconds(120);
    private static final long MESSAGE_SETTLE_TICKS = 20L;
    /** An island spot far from the run's island, for the region scenarios. */
    private static final String SCRATCH_ISLAND = "8192,8192";
    /** An island spot no island takes: the repair is to remove the nether region left there. */
    private static final String DELETED_ISLAND = "-8192,8192";
    /** A nether region of another id, which the repair is to leave. */
    private static final String OTHER_NETHER_REGION = "usbitplayernether";

    private final AtomicBoolean running = new AtomicBoolean();
    private final ScenarioLogCapture logCapture = new ScenarioLogCapture();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Logger.getLogger("").addHandler(logCapture);
    }

    @Override
    public void onDisable() {
        Logger.getLogger("").removeHandler(logCapture);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent ignored) {
        String configuredPhase = System.getProperty("uskyblock.ittest.phase", "").trim();
        if (!configuredPhase.isEmpty()) {
            Bukkit.getScheduler().runTask(this, () -> start(configuredPhase, true));
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("run")
            && (args[1].equalsIgnoreCase("fresh") || args[1].equalsIgnoreCase("restart"))) {
            start(args[1].toLowerCase(Locale.ROOT), false);
            return true;
        }
        sender.sendMessage("Usage: /" + label + " run <fresh|restart>");
        return true;
    }

    private void start(String phase, boolean shutdownWhenDone) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Harness start must run on the server thread");
        if (!running.compareAndSet(false, true)) {
            getLogger().warning("USKYBLOCK-TEST harness is already running");
            return;
        }
        if (!phase.equals("fresh") && !phase.equals("restart")) {
            getLogger().severe("USKYBLOCK-TEST invalid phase: " + phase);
            running.set(false);
            return;
        }
        Path resultDirectory = Path.of(System.getProperty(
            "uskyblock.ittest.resultsDir",
            getDataFolder().toPath().resolve("results").toString()
        )).toAbsolutePath().normalize();
        PhaseRunner runner = new PhaseRunner(phase, resultDirectory, shutdownWhenDone);
        runner.run();
    }

    private final class PhaseRunner {
        private final String phase;
        private final Path resultDirectory;
        private final boolean shutdownWhenDone;
        private final AtomicVerdictWriter writer;
        private final List<ScenarioDefinition> scenarios = new ArrayList<>();
        private int scenarioIndex;

        private PhaseRunner(String phase, Path resultDirectory, boolean shutdownWhenDone) {
            this.phase = phase;
            this.resultDirectory = resultDirectory;
            this.shutdownWhenDone = shutdownWhenDone;
            this.writer = new AtomicVerdictWriter(resultDirectory.resolve(phase + ".jsonl"));
            scenarios.add(new ScenarioDefinition("harness-canary", Verdict.Category.HARNESS_ERROR, this::canary));
            scenarios.add(new ScenarioDefinition("message-delivery", Verdict.Category.PLUGIN_FAIL, this::messageDelivery));
            if (phase.equals("fresh")) {
                scenarios.add(new ScenarioDefinition("initial-setup", Verdict.Category.DEPENDENCY_FAIL, this::initialSetup));
                if (Boolean.parseBoolean(System.getProperty("uskyblock.ittest.playerFlows", "true"))) {
                    scenarios.add(new ScenarioDefinition("create-island", Verdict.Category.PLUGIN_FAIL, this::createIsland));
                    scenarios.add(new ScenarioDefinition("complete-challenge", Verdict.Category.PLUGIN_FAIL, this::completeChallenge));
                    scenarios.add(new ScenarioDefinition("nether-ban", Verdict.Category.PLUGIN_FAIL, this::netherBan));
                    scenarios.add(new ScenarioDefinition("nether-lock", Verdict.Category.PLUGIN_FAIL, this::netherLock));
                    // The repair itself runs at the start of the restart phase.
                    scenarios.add(new ScenarioDefinition("island-region-repair-setup", Verdict.Category.PLUGIN_FAIL, this::islandRegionRepairSetup));
                } else {
                    scenarios.add(new ScenarioDefinition("player-flows-support", Verdict.Category.HARNESS_ERROR,
                        scenario -> scenario.skip("player flows disabled: no MCProtocolLib codec for this Minecraft version (server-only run)")));
                }
                scenarios.add(new ScenarioDefinition("island-regions-delete", Verdict.Category.PLUGIN_FAIL, this::islandRegionsDelete));
            } else {
                scenarios.add(new ScenarioDefinition("restart-persistence", Verdict.Category.PLUGIN_FAIL, this::restartPersistence));
                scenarios.add(new ScenarioDefinition("island-region-repair", Verdict.Category.PLUGIN_FAIL, this::islandRegionRepair));
            }
            scenarios.add(new ScenarioDefinition("secondary-smokes", Verdict.Category.PLUGIN_FAIL, this::secondarySmokes));
        }

        private void run() {
            runNext();
        }

        private void runNext() {
            if (scenarioIndex >= scenarios.size()) {
                getLogger().info("USKYBLOCK-TEST PHASE-END phase=" + phase);
                running.set(false);
                if (shutdownWhenDone) Bukkit.getScheduler().runTaskLater(IntegrationTestPlugin.this, Bukkit::shutdown, 2L);
                return;
            }
            ScenarioDefinition definition = scenarios.get(scenarioIndex++);
            Scenario scenario = new Scenario(definition.name(), definition.defaultCategory(), this::runNext);
            logCapture.begin(phase, definition.name());
            getLogger().info("USKYBLOCK-TEST START phase=" + phase + " scenario=" + definition.name());
            try {
                definition.action().run(scenario);
            } catch (Throwable throwable) {
                scenario.fail(definition.defaultCategory(), throwable);
            }
        }

        private void canary(Scenario scenario) {
            check(Bukkit.isPrimaryThread(), "scenario did not execute on the server thread");
            Bukkit.getScheduler().runTaskLater(IntegrationTestPlugin.this, () -> {
                try {
                    check(Bukkit.isPrimaryThread(), "delayed task did not execute on the server thread");
                    Verdict probe = new Verdict(1, phase, "canary-probe", Verdict.Result.PASS, 0, Verdict.Category.NONE, "round trip");
                    check(VerdictCodec.decode(VerdictCodec.encode(probe)).equals(probe), "verdict JSON round trip failed");
                    Path probeFile = resultDirectory.resolve(phase + "-atomic-probe.jsonl");
                    AtomicVerdictWriter probeWriter = new AtomicVerdictWriter(probeFile);
                    probeWriter.append(probe);
                    check(VerdictCodec.decode(Files.readString(probeFile).trim()).equals(probe), "atomic result parse failed");
                    Files.deleteIfExists(probeFile);
                    scenario.pass("server-thread, scheduler, and atomic verdict round trip succeeded");
                } catch (Throwable throwable) {
                    scenario.fail(Verdict.Category.HARNESS_ERROR, throwable);
                }
            }, 2L);
        }

        private void initialSetup(Scenario scenario) {
            scenario.await(this::coreReady, SETUP_TIMEOUT, () -> {
                for (String name : List.of("WorldEdit", "WorldGuard", "uSkyBlock", "uSkyBlock-itTest")) {
                    Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
                    check(plugin != null && plugin.isEnabled(), name + " is not enabled");
                }
                uSkyBlock usb = requireUsb();
                check(usb.getChallengeLogic() != null, "challenge service is unavailable");
                check(usb.getPlayerLogic() != null, "player service is unavailable");
                check(usb.getIslandLogic() != null, "island service is unavailable");
                check(usb.getRuntimeConfigs() != null && usb.getRuntimeConfigs().current() != null, "runtime configuration is unavailable");
                World sky = usb.getWorldManager().getWorld();
                check(sky != null, "sky world was not created");
                ChunkGenerator expected = usb.getDefaultWorldGenerator(sky.getName(), null);
                check(sky.getGenerator() != null && sky.getGenerator().getClass().getName().equals(expected.getClass().getName()),
                    "sky world does not use the uSkyBlock generator");
                if (usb.getRuntimeConfigs().current().nether().enabled()) {
                    World nether = usb.getWorldManager().getNetherWorld();
                    check(nether != null, "configured sky nether world was not created");
                    ChunkGenerator expectedNether = usb.getDefaultWorldGenerator(nether.getName(), null);
                    check(nether.getGenerator() != null
                            && nether.getGenerator().getClass().getName().equals(expectedNether.getClass().getName()),
                        "sky nether world does not use the uSkyBlock generator");
                }
                check(usb.getRuntimeConfigs().current().islandScheme(SCHEME_ID) != null, "fixture scheme was not loaded");
                check(usb.getChallengeLogic().getChallengeById(CHALLENGE_ID).isPresent(), "fixture challenge was not loaded");
                Path schematic = usb.getDataFolder().toPath().resolve("schematics/ittest.schematic");
                check(Files.isRegularFile(schematic) && Files.isReadable(schematic), "fixture schematic is missing or unreadable");
                String expectedRevision = System.getProperty("uskyblock.ittest.fixtureRevision", "1");
                check(Files.exists(resultDirectory.resolve("fixture-revision-" + expectedRevision)), "fixture revision marker is missing");
                scenario.pass("dependencies, services, generator, configuration, challenge, and schematic are ready");
            }, "uSkyBlock did not complete initialization before the deadline");
        }

        private boolean coreReady() {
            try {
                uSkyBlock usb = uSkyBlock.getInstance();
                return usb != null && usb.isEnabled() && usb.getRuntimeConfigs() != null
                    && usb.getChallengeLogic() != null && usb.getWorldManager() != null
                    && usb.getWorldManager().getWorld() != null;
            } catch (Throwable ignored) {
                return false;
            }
        }

        private void createIsland(Scenario scenario) {
            scenario.await(() -> Bukkit.getPlayerExact(PLAYER_NAME) != null, PLAYER_TIMEOUT, () -> {
                Player player = Objects.requireNonNull(Bukkit.getPlayerExact(PLAYER_NAME));
                check(player.getUniqueId().equals(PLAYER_UUID), "fixture player UUID mismatch: " + player.getUniqueId());
                uSkyBlock usb = requireUsb();
                PlayerInfo before = usb.getPlayerInfo(player);
                check(before != null, "player record was not created on join");
                check(!before.getHasIsland(), "fresh fixture player already owns an island");
                usb.createIsland(player, SCHEME_ID);
                scenario.await(() -> {
                    PlayerInfo current = usb.getPlayerInfo(PLAYER_UUID);
                    return current != null && !current.isIslandGenerating() && current.getHasIsland();
                }, ISLAND_TIMEOUT, () -> validateCreatedIsland(scenario, player), "island generation did not finish before the deadline");
            }, "fixture player did not connect before the deadline", Verdict.Category.HARNESS_ERROR);
        }

        private void validateCreatedIsland(Scenario scenario, Player player) {
            uSkyBlock usb = requireUsb();
            PlayerInfo playerInfo = usb.getPlayerInfo(PLAYER_UUID);
            check(playerInfo != null && playerInfo.getHasIsland(), "player island record is missing");
            IslandInfo island = usb.getIslandInfo(playerInfo);
            check(island != null, "island record is missing");
            OfflinePlayer offline = Bukkit.getOfflinePlayer(PLAYER_UUID);
            check(island.isLeader(PLAYER_UUID), "fixture player is not the island owner");
            check(island.isMember(offline), "fixture player is not an island member");
            Location islandLocation = playerInfo.getIslandLocation();
            Location home = playerInfo.getHomeLocation();
            check(validLocation(islandLocation), "island location is invalid");
            check(validLocation(home), "home location is invalid");
            check(validLocation(Objects.requireNonNull(islandLocation.getWorld()).getSpawnLocation()), "sky-world spawn location is invalid");
            check(island.contains(home), "home is outside the island");
            ProtectedRegion region = WorldGuardHandler.getIslandRegionAt(islandLocation);
            check(region != null && region.contains(islandLocation.getBlockX(), islandLocation.getBlockY(), islandLocation.getBlockZ()),
                "WorldGuard region does not cover the island");
            Block marker = findMarker(islandLocation).orElseThrow(() -> new AssertionError("schematic did not place a marker block"));
            check(player.teleport(home), "could not teleport fixture player to its island");

            Properties state = new Properties();
            state.setProperty("schema", "1");
            state.setProperty("playerName", PLAYER_NAME);
            state.setProperty("playerUuid", PLAYER_UUID.toString());
            state.setProperty("islandName", island.getName());
            putLocation(state, "home", home);
            putLocation(state, "island", islandLocation);
            state.setProperty("marker.world", marker.getWorld().getName());
            state.setProperty("marker.x", Integer.toString(marker.getX()));
            state.setProperty("marker.y", Integer.toString(marker.getY()));
            state.setProperty("marker.z", Integer.toString(marker.getZ()));
            state.setProperty("marker.material", marker.getType().getKey().toString());
            writeState(state);
            scenario.pass("island owner, membership, locations, marker, teleport, and WorldGuard region verified");
        }

        private void completeChallenge(Scenario scenario) {
            Player player = Bukkit.getPlayerExact(PLAYER_NAME);
            check(player != null && player.isOnline(), "fixture player disconnected before challenge execution");
            uSkyBlock usb = requireUsb();
            PlayerInfo playerInfo = usb.getPlayerInfo(player);
            check(playerInfo != null && playerInfo.getHasIsland(), "fixture player has no island");
            Location home = playerInfo.getHomeLocation();
            check(validLocation(home) && player.teleport(home), "could not place player on its island");
            ChallengeLogic logic = usb.getChallengeLogic();
            Challenge challenge = logic.getChallengeById(CHALLENGE_ID).orElseThrow();
            player.getInventory().clear();
            player.getInventory().addItem(new ItemStack(Material.STONE, 5));
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));
            logic.completeChallenge(player, challenge.getId());
            scenario.await(() -> logic.checkChallenge(playerInfo, CHALLENGE_ID) == 1, Duration.ofSeconds(15), () -> {
                check(count(player, Material.STONE) == 0, "required items were not consumed");
                check(count(player, Material.EMERALD) == 2, "deterministic item reward was not delivered");
                check(count(player, Material.DIAMOND) == 3, "unrelated inventory changed");
                check(logic.flushCache() > 0, "challenge completion cache did not contain saved state");
                check(logic.checkChallenge(playerInfo, CHALLENGE_ID) == 1,
                    "challenge completion was not readable through the service after a save/reload");
                Properties state = readState();
                state.setProperty("challengeId", CHALLENGE_ID.id());
                state.setProperty("challengeCompletions", "1");
                writeState(state);
                scenario.pass("challenge completion, item consumption, reward, and unrelated inventory verified");
            }, "challenge completion was not visible before the deadline");
        }

        private void restartPersistence(Scenario scenario) {
            scenario.await(this::coreReady, SETUP_TIMEOUT, () -> {
                Properties state = readState();
                check(PLAYER_UUID.toString().equals(state.getProperty("playerUuid")), "persisted fixture UUID mismatch");
                uSkyBlock usb = requireUsb();
                PlayerInfo playerInfo = usb.getPlayerInfo(PLAYER_UUID);
                check(playerInfo != null && playerInfo.getHasIsland(), "player island record did not survive restart");
                IslandInfo island = usb.getIslandInfo(playerInfo);
                check(island != null, "island record did not survive restart");
                OfflinePlayer offline = Bukkit.getOfflinePlayer(PLAYER_UUID);
                check(island.getName().equals(state.getProperty("islandName")), "island identity changed after restart");
                check(island.isLeader(PLAYER_UUID), "island ownership did not survive restart");
                check(island.isMember(offline), "island membership did not survive restart");
                Location home = playerInfo.getHomeLocation();
                check(sameBlock(home, state, "home"), "home location did not survive restart");
                Location islandLocation = playerInfo.getIslandLocation();
                ProtectedRegion region = WorldGuardHandler.getIslandRegionAt(islandLocation);
                check(region != null && region.contains(islandLocation.getBlockX(), islandLocation.getBlockY(), islandLocation.getBlockZ()),
                    "WorldGuard island region did not survive restart");
                World markerWorld = Bukkit.getWorld(requireProperty(state, "marker.world"));
                check(markerWorld != null, "marker world was not loaded");
                Block marker = markerWorld.getBlockAt(integer(state, "marker.x"), integer(state, "marker.y"), integer(state, "marker.z"));
                check(marker.getType().getKey().toString().equals(requireProperty(state, "marker.material")),
                    "schematic marker did not survive restart");
                ChallengeKey challengeId = ChallengeKey.of(requireProperty(state, "challengeId"));
                int expected = integer(state, "challengeCompletions");
                check(usb.getChallengeLogic().checkChallenge(playerInfo, challengeId) == expected,
                    "challenge completion count did not survive restart");
                scenario.pass("island, owner, membership, home, region, marker, and challenge progress survived restart");
            }, "uSkyBlock did not complete restart initialization before the deadline");
        }

        /**
         * Keeps a banned player off the nether island: {@code /is ban} sends the visitor from the fixture player's
         * nether island to spawn, a teleport back onto it is refused, and a move into its nether region is cancelled
         * with a bounce level with the ground, without an exception.
         */
        private void netherBan(Scenario scenario) {
            scenario.await(() -> Bukkit.getPlayerExact(VISITOR_NAME) != null, PLAYER_TIMEOUT, () -> {
                uSkyBlock usb = requireUsb();
                Player owner = requirePlayer(PLAYER_NAME);
                Player visitor = requirePlayer(VISITOR_NAME);
                // The bot does not move by itself: it stays where it is put, also in the air.
                visitor.setInvulnerable(true);
                // Loaded a tick before the ban: loading a banned player's data sends them to spawn by itself.
                usb.getPlayerInfo(visitor);
                PlayerInfo ownerInfo = usb.getPlayerInfo(owner);
                IslandInfo island = usb.getIslandInfo(ownerInfo);
                Location onTheNetherIsland = onTheNetherIsland(ownerInfo);
                check(visitor.teleport(onTheNetherIsland), "the visitor could not teleport onto the nether island");
                long teleported = visitor.getWorld().getGameTime();
                scenario.await(() -> visitor.getWorld().getGameTime() > teleported + 1, Duration.ofSeconds(10), () -> {
                    check(owner.performCommand("is ban " + VISITOR_NAME) && island.isBanned(visitor.getUniqueId()),
                        "/is ban did not ban the visitor");
                    Location spawn = usb.getWorldManager().getWorld().getSpawnLocation();
                    BooleanSupplier atSpawn = () -> visitor.getWorld().equals(spawn.getWorld())
                        && visitor.getLocation().distanceSquared(spawn) < 9;
                    scenario.await(atSpawn, Duration.ofSeconds(20), () -> {
                        check(!visitor.teleport(onTheNetherIsland), "the banned visitor could teleport onto the nether island");
                        Location outside = onTheNetherIsland.clone().add(usb.getRuntimeConfigs().current().island().radius() + 8, 0, 0);
                        PlayerMoveEvent move = new PlayerMoveEvent(visitor, outside, onTheNetherIsland);
                        Bukkit.getPluginManager().callEvent(move);
                        check(move.isCancelled(), "the banned visitor could move into the nether region");
                        Vector bounce = visitor.getVelocity();
                        check(bounce.getY() == 0 && Math.abs(bounce.length() - 1.5) < 1e-6,
                            "the banned visitor was not bounced level with the ground: " + bounce);
                        scenario.pass("/is ban sent the visitor from the nether island to spawn, the teleport back was"
                            + " refused, and the move into the nether region bounced them level with the ground");
                    }, "/is ban did not send the visitor from the nether island to spawn");
                }, "the nether island world did not tick");
            }, "the visitor did not connect before the deadline", Verdict.Category.HARNESS_ERROR);
        }

        /**
         * {@code /is lock} and {@code /is unlock} reach the island's regions in both island worlds at once, and the
         * visitor's teleport onto the locked nether island is refused.
         */
        private void netherLock(Scenario scenario) {
            uSkyBlock usb = requireUsb();
            Player owner = requirePlayer(PLAYER_NAME);
            Player visitor = requirePlayer(VISITOR_NAME);
            PlayerInfo ownerInfo = usb.getPlayerInfo(owner);
            IslandInfo island = usb.getIslandInfo(ownerInfo);
            if (island.isBanned(visitor.getUniqueId())) {
                check(owner.performCommand("is unban " + VISITOR_NAME) && !island.isBanned(visitor.getUniqueId()),
                    "/is unban did not unban the visitor");
            }
            // Fetched after the unban, which rebuilds the regions.
            ProtectedRegion region = regions(usb.getWorldManager().getWorld()).getRegion(island.getName() + "island");
            ProtectedRegion netherRegion = regions(netherWorld(usb)).getRegion(island.getName() + "nether");
            check(region != null && netherRegion != null, "the island is missing a region");
            Location onTheNetherIsland = onTheNetherIsland(ownerInfo);
            check(owner.performCommand("is lock") && island.isLocked(), "/is lock did not lock the island");
            check(region.getFlag(Flags.ENTRY) == StateFlag.State.DENY, "/is lock did not lock the island world's region");
            check(netherRegion.getFlag(Flags.ENTRY) == StateFlag.State.DENY, "/is lock did not lock the nether region");
            check(!visitor.teleport(onTheNetherIsland), "the visitor could teleport onto the locked nether island");
            check(owner.performCommand("is unlock") && !island.isLocked(), "/is unlock did not unlock the island");
            check(region.getFlag(Flags.ENTRY) == null, "/is unlock did not unlock the island world's region");
            check(netherRegion.getFlag(Flags.ENTRY) == null, "/is unlock did not unlock the nether region");
            check(visitor.teleport(onTheNetherIsland), "the visitor could not teleport onto the unlocked nether island");
            check(visitor.teleport(usb.getWorldManager().getWorld().getSpawnLocation()), "could not send the visitor back to spawn");
            scenario.pass("/is lock and /is unlock reached both regions at once, and the teleport onto the locked nether island was refused");
        }

        /**
         * A purge and a delete remove a scratch island's regions, each from its own world: the nether region from the
         * nether island world.
         */
        private void islandRegionsDelete(Scenario scenario) {
            uSkyBlock usb = requireUsb();
            check(!Files.exists(islandFile(usb, SCRATCH_ISLAND)), "the scratch island " + SCRATCH_ISLAND + " is taken");
            RegionManager overworld = regions(usb.getWorldManager().getWorld());
            RegionManager nether = regions(netherWorld(usb));
            String regionId = SCRATCH_ISLAND + "island";
            String netherRegionId = SCRATCH_ISLAND + "nether";
            WorldGuardHandler.updateRegion(usb.getIslandInfo(SCRATCH_ISLAND));
            check(overworld.hasRegion(regionId) && nether.hasRegion(netherRegionId), "the scratch island did not get its regions");
            check(usb.getIslandLogic().purge(SCRATCH_ISLAND), "the purge refused the scratch island");
            check(!overworld.hasRegion(regionId), "the purge left the island world's region");
            check(!nether.hasRegion(netherRegionId), "the purge left the nether region");
            WorldGuardHandler.updateRegion(usb.getIslandInfo(SCRATCH_ISLAND));
            check(overworld.hasRegion(regionId) && nether.hasRegion(netherRegionId), "the scratch island did not get its regions again");
            AtomicBoolean deleted = new AtomicBoolean();
            check(usb.deleteEmptyIsland(SCRATCH_ISLAND, () -> deleted.set(true)), "the delete refused the scratch island");
            scenario.await(deleted::get, Duration.ofSeconds(60), () -> {
                check(!overworld.hasRegion(regionId), "the delete left the island world's region");
                check(!nether.hasRegion(netherRegionId), "the delete left the nether region");
                scenario.pass("a purge and a delete removed the scratch island's regions, each from its own world");
            }, "the delete of the scratch island did not finish before the deadline");
        }

        /**
         * Leaves the restart phase's start what the island region repair is for: the nether region of a deleted
         * island, still locked and owned, and the run island's nether region locked although the island is not, as
         * {@code /is unlock} left it. The repair ran at this phase's start already, on a server without islands, so its
         * marker goes.
         */
        private void islandRegionRepairSetup(Scenario scenario) {
            uSkyBlock usb = requireUsb();
            Path marker = repairMarker(usb);
            check(Files.exists(marker), "the island region repair did not run at the start");
            IslandInfo island = usb.getIslandInfo(usb.getPlayerInfo(PLAYER_UUID));
            check(island != null && !island.isLocked(), "the run island is missing or locked");
            RegionManager nether = regions(netherWorld(usb));
            ProtectedRegion netherRegion = nether.getRegion(island.getName() + "nether");
            check(netherRegion != null, "the run island has no nether region");
            netherRegion.setFlag(Flags.ENTRY, StateFlag.State.DENY);
            check(!Files.exists(islandFile(usb, DELETED_ISLAND))
                    && !regions(usb.getWorldManager().getWorld()).hasRegion(DELETED_ISLAND + "island"),
                "the island " + DELETED_ISLAND + " exists");
            Location deleted = new Location(netherWorld(usb), -8192, 75, 8192);
            ProtectedCuboidRegion orphan = new ProtectedCuboidRegion(DELETED_ISLAND + "nether",
                WorldGuardHandler.getProtectionVectorRight(deleted), WorldGuardHandler.getProtectionVectorLeft(deleted));
            orphan.getOwners().addPlayer(UUID.nameUUIDFromBytes("OfflinePlayer:UsbItGone".getBytes(StandardCharsets.UTF_8)));
            orphan.setFlag(Flags.ENTRY, StateFlag.State.DENY);
            nether.addRegion(orphan);
            nether.addRegion(new ProtectedCuboidRegion(OTHER_NETHER_REGION,
                WorldGuardHandler.getProtectionVectorRight(deleted), WorldGuardHandler.getProtectionVectorLeft(deleted)));
            try {
                nether.saveChanges();
                Files.delete(marker);
            } catch (StorageException | IOException e) {
                throw new IllegalStateException("could not leave the island region repair its work", e);
            }
            scenario.pass("left a deleted island's nether region and a stale lock on the run island's nether region");
        }

        /**
         * The island region repair, at the first start after its marker went: it removed the deleted island's nether
         * region and left the region of another id, and unlocked the run island's nether region, as the island is.
         */
        private void islandRegionRepair(Scenario scenario) {
            scenario.await(() -> coreReady() && Files.exists(repairMarker(uSkyBlock.getInstance())), SETUP_TIMEOUT, () -> {
                uSkyBlock usb = requireUsb();
                RegionManager nether = regions(netherWorld(usb));
                check(!nether.hasRegion(DELETED_ISLAND + "nether"), "the repair left the deleted island's nether region");
                check(nether.hasRegion(OTHER_NETHER_REGION), "the repair removed a region of another id");
                IslandInfo island = usb.getIslandInfo(usb.getPlayerInfo(PLAYER_UUID));
                check(island != null && !island.isLocked(), "the run island is missing or locked");
                ProtectedRegion netherRegion = nether.getRegion(island.getName() + "nether");
                check(netherRegion != null, "the repair removed the run island's nether region");
                check(netherRegion.getFlag(Flags.ENTRY) == null, "the repair left the unlocked island's nether region locked");
                check(netherRegion.getOwners().contains(PLAYER_UUID), "the run island's nether region lost its owner");
                check(regions(usb.getWorldManager().getWorld()).hasRegion(island.getName() + "island"),
                    "the run island lost its region in the island world");
                scenario.pass("the repair removed the deleted island's nether region and synced the run island's lock to its nether region");
            }, "the island region repair did not finish before the deadline");
        }

        private void secondarySmokes(Scenario scenario) {
            uSkyBlock usb = requireUsb();
            ChallengeLogic logic = usb.getChallengeLogic();
            List<ChallengeKey> ids = logic.getAllChallengeIds();
            check(!ids.isEmpty(), "no challenges were parsed");
            for (ChallengeKey id : ids) check(logic.getChallengeById(id).isPresent(), "challenge index could not resolve " + id.id());
            Challenge fixture = logic.getChallengeById(CHALLENGE_ID).orElseThrow();
            ChallengeCompletion completion = new ChallengeCompletion(CHALLENGE_ID, null, 0, 0);
            ItemStack display = fixture.getDisplayItem(completion, false);
            check(display.getType() != Material.AIR && display.hasItemMeta(), "challenge display item has unusable metadata");
            var schemes = usb.getRuntimeConfigs().current().islandSchemes();
            check(!schemes.isEmpty(), "no island schematics were configured");
            for (var entry : schemes.entrySet()) {
                if (!entry.getValue().enabled()) continue;
                String configured = entry.getValue().schematic();
                check(configured != null && !configured.isBlank(), "scheme " + entry.getKey() + " has no schematic");
                Path path = usb.getDataFolder().toPath().resolve("schematics").resolve(configured).normalize();
                check(path.startsWith(usb.getDataFolder().toPath().resolve("schematics").normalize()) && Files.isReadable(path),
                    "scheme " + entry.getKey() + " schematic is unreadable");
            }
            scenario.pass("all challenge IDs and configured schematics parsed; display item rendered");
        }

        /**
         * Pins the message-delivery path: a {@link Component} handed to {@link Msg} must actually
         * reach the sender.
         *
         * <p>Every player-facing string in uSkyBlock travels
         * {@code Msg -> NotificationManager -> adventure-platform}. When no platform facet applies,
         * adventure returns {@code Audience.empty()}, which discards messages silently - no
         * exception, no console output, no clue for the player. The plugin then looks alive while
         * being entirely mute, which is indistinguishable from a command bug.</p>
         *
         * <p>This scenario sends a unique marker to the console sender through the configured
         * delivery and asserts it is emitted. It deliberately does not touch NotificationManager's
         * internals, so it stays valid across changes to how delivery is implemented.</p>
         */
        private void messageDelivery(Scenario scenario) {
            requireUsb();
            String marker = "usb-ittest-msg-" + UUID.randomUUID();
            AtomicBoolean seen = new AtomicBoolean();

            org.apache.logging.log4j.core.Logger root;
            try {
                root = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
            } catch (Throwable throwable) {
                scenario.skip("cannot observe console output on this server: " + throwable);
                return;
            }

            AbstractAppender appender = new AbstractAppender(marker, null, null, true, Property.EMPTY_ARRAY) {
                @Override
                public void append(LogEvent event) {
                    if (event == null || event.getMessage() == null) return;
                    if (event.getMessage().getFormattedMessage().contains(marker)) seen.set(true);
                }
            };
            appender.start();
            root.addAppender(appender);

            try {
                Msg.send(Bukkit.getConsoleSender(), Component.text(marker));
                Player player = Bukkit.getPlayerExact(PLAYER_NAME);
                if (player != null) Msg.send(player, Component.text(marker + "-player"));
            } catch (Throwable throwable) {
                root.removeAppender(appender);
                appender.stop();
                scenario.fail(Verdict.Category.PLUGIN_FAIL, throwable);
                return;
            }

            // Console appenders may be asynchronous; settle before judging.
            Bukkit.getScheduler().runTaskLater(IntegrationTestPlugin.this, () -> {
                root.removeAppender(appender);
                appender.stop();
                if (seen.get()) {
                    scenario.pass("console message delivered through the configured Msg delivery");
                } else {
                    scenario.fail(Verdict.Category.PLUGIN_FAIL,
                        "a Component sent through Msg never reached the console: the configured delivery "
                            + "discarded it silently (adventure-platform yields Audience.empty() when no facet applies)");
                }
            }, MESSAGE_SETTLE_TICKS);
        }

        private final class Scenario {
            private final String name;
            private final Verdict.Category defaultCategory;
            private final Runnable continuation;
            private final long started = System.nanoTime();
            private final AtomicBoolean finished = new AtomicBoolean();

            private Scenario(String name, Verdict.Category defaultCategory, Runnable continuation) {
                this.name = name;
                this.defaultCategory = defaultCategory;
                this.continuation = continuation;
            }

            void await(BooleanSupplier condition, Duration timeout, Runnable success, String timeoutDetail) {
                await(condition, timeout, success, timeoutDetail, defaultCategory);
            }

            void await(BooleanSupplier condition, Duration timeout, Runnable success, String timeoutDetail, Verdict.Category timeoutCategory) {
                long expires = System.nanoTime() + timeout.toNanos();
                class Poll implements Runnable {
                    @Override
                    public void run() {
                        if (finished.get()) return;
                        try {
                            if (condition.getAsBoolean()) {
                                success.run();
                            } else if (System.nanoTime() >= expires) {
                                fail(timeoutCategory, timeoutDetail);
                            } else {
                                Bukkit.getScheduler().runTaskLater(IntegrationTestPlugin.this, this, 1L);
                            }
                        } catch (Throwable throwable) {
                            fail(defaultCategory, throwable);
                        }
                    }
                }
                new Poll().run();
            }

            void pass(String detail) {
                finish(Verdict.Result.PASS, Verdict.Category.NONE, detail);
            }

            void skip(String detail) {
                finish(Verdict.Result.SKIP, Verdict.Category.NONE, detail);
            }

            void fail(Verdict.Category category, String detail) {
                finish(Verdict.Result.FAIL, category, detail);
            }

            void fail(Verdict.Category category, Throwable throwable) {
                StringWriter trace = new StringWriter();
                throwable.printStackTrace(new PrintWriter(trace));
                fail(category, throwable.getClass().getSimpleName() + ": " + Optional.ofNullable(throwable.getMessage()).orElse("") + " | " + trace);
            }

            private void finish(Verdict.Result result, Verdict.Category category, String detail) {
                if (!finished.compareAndSet(false, true)) return;
                List<String> logged = logCapture.end();
                if (!logged.isEmpty()) {
                    detail = detail + " | captured logs: " + String.join(" || ", logged);
                    if (result == Verdict.Result.PASS) {
                        result = Verdict.Result.FAIL;
                        category = defaultCategory;
                    }
                }
                long duration = Duration.ofNanos(System.nanoTime() - started).toMillis();
                Verdict verdict = new Verdict(1, phase, name, result, duration, category, detail);
                try {
                    writer.append(verdict);
                    getLogger().info("USKYBLOCK-TEST END phase=" + phase + " scenario=" + name + " result=" + result
                        + " category=" + category.name().replace('_', '-') + " durationMs=" + duration);
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "USKYBLOCK-TEST could not finalize result", e);
                }
                Bukkit.getScheduler().runTask(IntegrationTestPlugin.this, continuation);
            }
        }

        private record ScenarioDefinition(String name, Verdict.Category defaultCategory, ScenarioAction action) { }
        @FunctionalInterface private interface ScenarioAction { void run(Scenario scenario); }

        private uSkyBlock requireUsb() {
            return Objects.requireNonNull(uSkyBlock.getInstance(), "uSkyBlock instance is unavailable");
        }

        private Player requirePlayer(String name) {
            Player player = Bukkit.getPlayerExact(name);
            check(player != null && player.isOnline(), name + " is not online");
            return player;
        }

        private void writeState(Properties state) {
            Path target = resultDirectory.resolve("state.properties");
            try {
                Files.createDirectories(target.getParent());
                Path temporary = Files.createTempFile(target.getParent(), "state", ".tmp");
                try (var output = Files.newOutputStream(temporary)) {
                    state.store(output, "uSkyBlock ittest stable identifiers");
                }
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            } catch (IOException e) {
                throw new IllegalStateException("could not write stable harness state", e);
            }
        }

        private Properties readState() {
            Properties state = new Properties();
            Path path = resultDirectory.resolve("state.properties");
            try (var input = Files.newInputStream(path)) {
                state.load(input);
            } catch (IOException e) {
                throw new IllegalStateException("could not read stable harness state", e);
            }
            check("1".equals(state.getProperty("schema")), "unsupported or missing state schema");
            return state;
        }
    }

    private static final class ScenarioLogCapture extends Handler {
        private final List<String> records = new ArrayList<>();
        private volatile boolean active;

        synchronized void begin(String phase, String scenario) {
            records.clear();
            active = true;
        }

        synchronized List<String> end() {
            active = false;
            return List.copyOf(records);
        }

        @Override
        public synchronized void publish(LogRecord record) {
            if (!active || record == null) return;
            if (record.getLevel().intValue() >= Level.SEVERE.intValue() || record.getThrown() != null) {
                String detail = record.getLoggerName() + ": " + record.getMessage();
                if (record.getThrown() != null) detail += " (" + record.getThrown().getClass().getName() + ")";
                records.add(VerdictCodec.sanitize(detail));
            }
        }

        @Override public void flush() { }
        @Override public void close() { }
    }

    private static boolean validLocation(Location location) {
        return location != null && location.getWorld() != null && Double.isFinite(location.getX())
            && Double.isFinite(location.getY()) && Double.isFinite(location.getZ())
            && location.getY() >= location.getWorld().getMinHeight() && location.getY() < location.getWorld().getMaxHeight();
    }

    /**
     * A spot in the nether island's region, off its centre and in the air above its blocks: away from its portal,
     * which would take a player standing in it back to the island world.
     */
    private static Location onTheNetherIsland(PlayerInfo playerInfo) {
        return Objects.requireNonNull(playerInfo.getIslandNetherLocation(), "the island has no nether location").add(6.5, 25, 6.5);
    }

    private static World netherWorld(uSkyBlock usb) {
        return Objects.requireNonNull(usb.getWorldManager().getNetherWorld(), "the nether island world is missing");
    }

    private static RegionManager regions(World world) {
        return Objects.requireNonNull(WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world)),
            "no regions in " + world.getName());
    }

    private static Path islandFile(uSkyBlock usb, String islandName) {
        return usb.getIslandLogic().getIslandDirectory().resolve(islandName + ".yml");
    }

    private static Path repairMarker(uSkyBlock usb) {
        return usb.getDataFolder().toPath().resolve("data").resolve("island-region-repair.done");
    }

    private static Optional<Block> findMarker(Location center) {
        World world = Objects.requireNonNull(center.getWorld());
        for (int y = center.getBlockY() - 12; y <= center.getBlockY() + 12; y++) {
            for (int x = center.getBlockX() - 12; x <= center.getBlockX() + 12; x++) {
                for (int z = center.getBlockZ() - 12; z <= center.getBlockZ() + 12; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isAir() && block.getType() != Material.WATER && block.getType() != Material.LAVA) return Optional.of(block);
                }
            }
        }
        return Optional.empty();
    }

    private static int count(Player player, Material material) {
        return player.getInventory().all(material).values().stream().mapToInt(ItemStack::getAmount).sum();
    }

    private static void putLocation(Properties state, String prefix, Location location) {
        state.setProperty(prefix + ".world", Objects.requireNonNull(location.getWorld()).getName());
        state.setProperty(prefix + ".x", Integer.toString(location.getBlockX()));
        state.setProperty(prefix + ".y", Integer.toString(location.getBlockY()));
        state.setProperty(prefix + ".z", Integer.toString(location.getBlockZ()));
    }

    private static boolean sameBlock(Location location, Properties state, String prefix) {
        return validLocation(location)
            && Objects.requireNonNull(location.getWorld()).getName().equals(requireProperty(state, prefix + ".world"))
            && location.getBlockX() == integer(state, prefix + ".x")
            && location.getBlockY() == integer(state, prefix + ".y")
            && location.getBlockZ() == integer(state, prefix + ".z");
    }

    private static int integer(Properties state, String key) {
        return Integer.parseInt(requireProperty(state, key));
    }

    private static String requireProperty(Properties state, String key) {
        String value = state.getProperty(key);
        if (value == null || value.isBlank()) throw new AssertionError("missing state property " + key);
        return value;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
