package us.talabrek.ultimateskyblock.ittest;

import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
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
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.async.Callback;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.challenge.ChallengeText;
import us.talabrek.ultimateskyblock.challenge.IslandBiomeUnlocks;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class IntegrationTestPlugin extends JavaPlugin implements Listener {
    static final String PLAYER_NAME = "UsbItPlayer";
    static final UUID PLAYER_UUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + PLAYER_NAME).getBytes(StandardCharsets.UTF_8));
    static final String SCHEME_ID = "ittest";
    static final ChallengeId CHALLENGE_ID = ChallengeId.of("ittest_trade");
    // One challenge per completion scenario (see scripts/ittest/fixtures/challenges.yml); distinct
    // ids keep the scenarios isolated even though they share one island and server.
    private static final ChallengeId INSUFFICIENT = ChallengeId.of("ittest_insufficient");
    private static final ChallengeId SURPLUS = ChallengeId.of("ittest_surplus");
    private static final ChallengeId KEEP = ChallengeId.of("ittest_keep");
    private static final ChallengeId ANYOF = ChallengeId.of("ittest_anyof");
    private static final ChallengeId NONREPEAT = ChallengeId.of("ittest_nonrepeat");
    private static final ChallengeId REPEATABLE = ChallengeId.of("ittest_repeatable");
    private static final ChallengeId XP = ChallengeId.of("ittest_xp");
    private static final ChallengeId COMMAND = ChallengeId.of("ittest_command");
    private static final ChallengeId BLOCKS = ChallengeId.of("ittest_blocks");
    private static final ChallengeId LEVEL = ChallengeId.of("ittest_level");
    private static final ChallengeId BIOME = ChallengeId.of("ittest_biome");
    private static final ChallengeId ADMIN = ChallengeId.of("ittest_admin");
    private static final ChallengeId PREREQ = ChallengeId.of("ittest_prereq");
    private static final ChallengeId GATED = ChallengeId.of("ittest_gated");
    private static final Duration SETUP_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration PLAYER_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration ISLAND_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration SETTLE_TIMEOUT = Duration.ofSeconds(15);
    // An integer-valued level, so it survives the YAML double round-trip exactly.
    private static final double PERSIST_LEVEL = 137.0;

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
            if (phase.equals("fresh")) {
                scenarios.add(new ScenarioDefinition("initial-setup", Verdict.Category.DEPENDENCY_FAIL, this::initialSetup));
                if (Boolean.parseBoolean(System.getProperty("uskyblock.ittest.playerFlows", "true"))) {
                    scenarios.add(new ScenarioDefinition("create-island", Verdict.Category.PLUGIN_FAIL, this::createIsland));
                    scenarios.add(new ScenarioDefinition("complete-challenge", Verdict.Category.PLUGIN_FAIL, this::completeChallenge));
                    // Challenge-completion edge cases, requirement types, and reward types. Each reuses
                    // the island created above and a distinct fixture challenge, so they stay isolated.
                    scenarios.add(new ScenarioDefinition("challenge-insufficient-items", Verdict.Category.PLUGIN_FAIL, this::challengeInsufficientItems));
                    scenarios.add(new ScenarioDefinition("challenge-surplus-items", Verdict.Category.PLUGIN_FAIL, this::challengeSurplusItems));
                    scenarios.add(new ScenarioDefinition("challenge-keep-items", Verdict.Category.PLUGIN_FAIL, this::challengeKeepItems));
                    scenarios.add(new ScenarioDefinition("challenge-any-of-combination", Verdict.Category.PLUGIN_FAIL, this::challengeAnyOfCombination));
                    scenarios.add(new ScenarioDefinition("challenge-non-repeatable", Verdict.Category.PLUGIN_FAIL, this::challengeNonRepeatable));
                    scenarios.add(new ScenarioDefinition("challenge-repeatable-limit", Verdict.Category.PLUGIN_FAIL, this::challengeRepeatableLimit));
                    scenarios.add(new ScenarioDefinition("challenge-island-blocks", Verdict.Category.PLUGIN_FAIL, this::challengeIslandBlocks));
                    scenarios.add(new ScenarioDefinition("challenge-island-level", Verdict.Category.PLUGIN_FAIL, this::challengeIslandLevel));
                    scenarios.add(new ScenarioDefinition("challenge-xp-reward", Verdict.Category.PLUGIN_FAIL, this::challengeXpReward));
                    scenarios.add(new ScenarioDefinition("challenge-command-reward", Verdict.Category.PLUGIN_FAIL, this::challengeCommandReward));
                    scenarios.add(new ScenarioDefinition("challenge-biome-reward", Verdict.Category.PLUGIN_FAIL, this::challengeBiomeReward));
                    scenarios.add(new ScenarioDefinition("challenge-unlock-gated", Verdict.Category.PLUGIN_FAIL, this::challengeUnlockGated));
                    scenarios.add(new ScenarioDefinition("challenge-admin-complete", Verdict.Category.PLUGIN_FAIL, this::challengeAdminComplete));
                    // A real chunk-snapshot level scan (unlike challenge-island-level, which fakes the
                    // level via setLevel). Exercises the version-sensitive scoring path a unit test cannot.
                    scenarios.add(new ScenarioDefinition("island-level-scan", Verdict.Category.PLUGIN_FAIL, this::islandLevelScan));
                    // Runs last, after any scenario that mutates island level, to arrange the level and
                    // warp state that the restart phase proves survives a real server restart.
                    scenarios.add(new ScenarioDefinition("persist-state", Verdict.Category.PLUGIN_FAIL, this::persistState));
                } else {
                    scenarios.add(new ScenarioDefinition("player-flows-support", Verdict.Category.HARNESS_ERROR,
                        scenario -> scenario.skip("player flows disabled: no MCProtocolLib codec for this Minecraft version (server-only run)")));
                }
            } else {
                scenarios.add(new ScenarioDefinition("restart-persistence", Verdict.Category.PLUGIN_FAIL, this::restartPersistence));
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
                check(usb.getChallengeLogic().getDefinitionById(CHALLENGE_ID).isPresent(), "fixture challenge was not loaded");
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
                // The island-generating flag clears in GenerateTask's deferred (teleportDelay) block,
                // which also performs the island-start inventory clear; the harness config sets
                // teleportDelay to 0 so that block runs promptly and cannot bleed into a later scenario.
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
            // The owner domain governs who can build; a silent break here would let anyone grief the
            // island. These flags are set by WorldGuardHandler.updateRegion and can only be observed
            // against a real RegionManager, so they belong in the live harness rather than a unit test.
            check(region.getOwners().contains(PLAYER_UUID), "island region owner domain does not include the fixture player");
            check(region.getPriority() == 100, "island region priority is not 100");
            check(region.getFlag(Flags.PVP) == null, "island region has an unexpected PVP flag override");
            check(region.getFlag(Flags.ENTRY) == null, "island region ENTRY flag should be unset for an unlocked island");
            Block marker = findMarker(islandLocation).orElseThrow(() -> new AssertionError("schematic did not place a marker block"));
            check(player.teleport(home), "could not teleport fixture player to its island");

            // Creating an island arms a one-time inventory clear (options.restart.clearInventory) that
            // uSkyBlock defers to one tick after the player first enters the skyworld. Let it fire and
            // settle here, on the freshly created island, so it cannot wipe the inventory precondition the
            // challenge scenario establishes later.
            scenario.await(() -> !playerInfo.isClearInventoryOnNextEntry(), SETTLE_TIMEOUT, () -> {
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
            }, "island-start inventory clear did not settle after entering the island");
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
            check(logic.getDefinitionById(CHALLENGE_ID).isPresent(), "fixture challenge was not loaded");
            player.getInventory().clear();
            player.getInventory().addItem(new ItemStack(Material.STONE, 5));
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));
            logic.completeChallenge(player, CHALLENGE_ID);
            scenario.await(() -> logic.checkChallenge(playerInfo, CHALLENGE_ID) == 1, Duration.ofSeconds(15), () -> {
                check(count(player, Material.STONE) == 0, "required items were not consumed");
                check(count(player, Material.EMERALD) == 2, "deterministic item reward was not delivered");
                check(count(player, Material.DIAMOND) == 3, "unrelated inventory changed");
                check(logic.flushCache() > 0, "challenge completion cache did not contain saved state");
                check(logic.checkChallenge(playerInfo, CHALLENGE_ID) == 1,
                    "challenge completion was not readable through the service after a save/reload");
                Properties state = readState();
                state.setProperty("challengeId", CHALLENGE_ID.value());
                state.setProperty("challengeCompletions", "1");
                writeState(state);
                scenario.pass("challenge completion, item consumption, reward, and unrelated inventory verified");
            }, "challenge completion was not visible before the deadline");
        }

        // Edge case: too few items -> the completion is rejected, nothing is consumed, and unrelated
        // inventory is left alone.
        private void challengeInsufficientItems(Scenario scenario) {
            Ctx c = onIsland();
            c.player().getInventory().clear();
            c.player().getInventory().addItem(new ItemStack(Material.STONE, 3));   // requirement asks for 10
            c.player().getInventory().addItem(new ItemStack(Material.DIAMOND, 2)); // unrelated bystander
            c.logic().completeChallenge(c.player(), INSUFFICIENT);
            afterTicks(scenario, 3, () -> {
                check(c.logic().checkChallenge(c.playerInfo(), INSUFFICIENT) == 0, "insufficient hand-in must not complete");
                check(count(c.player(), Material.STONE) == 3, "rejected completion must not consume items");
                check(count(c.player(), Material.DIAMOND) == 2, "rejected completion must not touch unrelated items");
                scenario.pass("insufficient hand-in was rejected without consuming any items");
            });
        }

        // Edge case: more items than required -> only the required amount is consumed, the surplus stays.
        private void challengeSurplusItems(Scenario scenario) {
            Ctx c = onIsland();
            c.player().getInventory().clear();
            c.player().getInventory().addItem(new ItemStack(Material.COBBLESTONE, 12)); // requirement asks for 5
            c.logic().completeChallenge(c.player(), SURPLUS);
            awaitCount(scenario, c, SURPLUS, 1, () -> {
                check(count(c.player(), Material.COBBLESTONE) == 7, "only the required five cobblestone should be consumed");
                check(count(c.player(), Material.EMERALD) == 1, "surplus challenge reward was not delivered");
                scenario.pass("surplus hand-in consumed exactly the required amount and left the rest");
            });
        }

        // consumeItemsOnCompletion=false -> the item must be present but is never removed.
        private void challengeKeepItems(Scenario scenario) {
            Ctx c = onIsland();
            c.player().getInventory().clear();
            c.player().getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
            c.logic().completeChallenge(c.player(), KEEP);
            awaitCount(scenario, c, KEEP, 1, () -> {
                check(count(c.player(), Material.DIAMOND) == 1, "consumeItemsOnCompletion=false must keep the handed-in item");
                scenario.pass("non-consuming challenge completed without removing the item");
            });
        }

        // Any-of matcher satisfied by a COMBINATION of different permitted variants (2 + 1 + 1 = 4),
        // proving the requirement aggregates across, and consumes from, multiple matching stacks.
        private void challengeAnyOfCombination(Scenario scenario) {
            Ctx c = onIsland();
            c.player().getInventory().clear();
            c.player().getInventory().addItem(new ItemStack(Material.OAK_LOG, 2));
            c.player().getInventory().addItem(new ItemStack(Material.BIRCH_LOG, 1));
            c.player().getInventory().addItem(new ItemStack(Material.SPRUCE_LOG, 1));
            c.logic().completeChallenge(c.player(), ANYOF);
            awaitCount(scenario, c, ANYOF, 1, () -> {
                check(count(c.player(), Material.OAK_LOG) == 0 && count(c.player(), Material.BIRCH_LOG) == 0
                    && count(c.player(), Material.SPRUCE_LOG) == 0, "the mixed permitted items should all be consumed");
                check(count(c.player(), Material.EMERALD) == 1, "any-of challenge reward was not delivered");
                scenario.pass("any-of requirement satisfied by a combination of permitted items");
            });
        }

        // A non-repeatable challenge cannot be completed a second time.
        private void challengeNonRepeatable(Scenario scenario) {
            Ctx c = onIsland();
            c.player().getInventory().clear();
            c.player().getInventory().addItem(new ItemStack(Material.IRON_INGOT, 3));
            c.logic().completeChallenge(c.player(), NONREPEAT);
            awaitCount(scenario, c, NONREPEAT, 1, () -> {
                c.player().getInventory().clear();
                c.player().getInventory().addItem(new ItemStack(Material.IRON_INGOT, 3));
                c.logic().completeChallenge(c.player(), NONREPEAT);
                afterTicks(scenario, 3, () -> {
                    check(c.logic().checkChallenge(c.playerInfo(), NONREPEAT) == 1, "non-repeatable challenge must stay at one completion");
                    check(count(c.player(), Material.IRON_INGOT) == 3, "rejected second attempt must not consume items");
                    scenario.pass("non-repeatable challenge rejected the second completion");
                });
            });
        }

        // A repeatable challenge (limit 2): first vs repeat reward differ, and a third completion within
        // the window is rejected. The completion count is persisted for the restart phase to verify.
        private void challengeRepeatableLimit(Scenario scenario) {
            Ctx c = onIsland();
            handInGold(c);
            c.logic().completeChallenge(c.player(), REPEATABLE);
            awaitCount(scenario, c, REPEATABLE, 1, () -> {
                check(count(c.player(), Material.EMERALD) == 1, "first completion should grant the first reward (emerald)");
                handInGold(c);
                c.logic().completeChallenge(c.player(), REPEATABLE);
                awaitCount(scenario, c, REPEATABLE, 2, () -> {
                    check(count(c.player(), Material.DIAMOND) == 1, "repeat completion should grant the repeat reward (diamond)");
                    check(count(c.player(), Material.EMERALD) == 0, "repeat completion should not re-grant the first reward");
                    handInGold(c);
                    c.logic().completeChallenge(c.player(), REPEATABLE);
                    afterTicks(scenario, 3, () -> {
                        check(c.logic().checkChallenge(c.playerInfo(), REPEATABLE) == 2, "repeat limit must cap completions at two");
                        check(count(c.player(), Material.GOLD_INGOT) == 1, "over-limit attempt must not consume items");
                        Properties state = readState();
                        state.setProperty("repeatableChallengeId", REPEATABLE.value());
                        state.setProperty("repeatableCompletions", "2");
                        writeState(state);
                        scenario.pass("repeatable challenge honored first/repeat rewards and the two-per-window limit");
                    });
                });
            });
        }

        private void handInGold(Ctx c) {
            c.player().getInventory().clear();
            c.player().getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 1));
        }

        // island-blocks requirement: rejected while the blocks are absent, completes once they are placed.
        private void challengeIslandBlocks(Scenario scenario) {
            Ctx c = onIsland();
            c.player().getInventory().clear();
            c.logic().completeChallenge(c.player(), BLOCKS);
            afterTicks(scenario, 3, () -> {
                check(c.logic().checkChallenge(c.playerInfo(), BLOCKS) == 0, "island-blocks challenge must reject when the blocks are absent");
                Location base = c.player().getLocation();
                List<Block> placed = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    Block block = base.clone().add(0, 3, i).getBlock(); // above the player's head, in open air
                    block.setType(Material.DIAMOND_BLOCK);
                    placed.add(block);
                }
                c.logic().completeChallenge(c.player(), BLOCKS);
                awaitCount(scenario, c, BLOCKS, 1, () -> {
                    check(count(c.player(), Material.EMERALD) == 1, "island-blocks reward was not delivered");
                    placed.forEach(block -> block.setType(Material.AIR)); // leave the scan area clean for later scenarios
                    scenario.pass("island-blocks challenge completed once the required blocks were present");
                });
            });
        }

        // island-level requirement: rejected below the threshold, completes once the level is raised.
        private void challengeIslandLevel(Scenario scenario) {
            Ctx c = onIsland();
            IslandInfo island = requireUsb().getIslandInfo(c.playerInfo());
            check(island != null, "island record missing for the island-level challenge");
            double originalLevel = island.getLevel();
            c.player().getInventory().clear();
            island.setLevel(0);
            c.logic().completeChallenge(c.player(), LEVEL);
            afterTicks(scenario, 3, () -> {
                check(c.logic().checkChallenge(c.playerInfo(), LEVEL) == 0, "island-level challenge must reject below the minimum level");
                island.setLevel(60); // requirement asks for 50
                c.logic().completeChallenge(c.player(), LEVEL);
                awaitCount(scenario, c, LEVEL, 1, () -> {
                    check(count(c.player(), Material.EMERALD) == 1, "island-level reward was not delivered");
                    island.setLevel(originalLevel);
                    scenario.pass("island-level challenge respected the minimum level requirement");
                });
            });
        }

        // Drives a REAL asynchronous chunk-snapshot scan (unlike challenge-island-level, which fakes the
        // level with setLevel): places high-value blocks and asserts the computed level rises.
        // calculateScoreAsync writes island.getLevel() from the scan result, so this exercises the
        // version-sensitive ChunkSnapshot + scoring path end to end - the path a unit test cannot fake.
        private void islandLevelScan(Scenario scenario) {
            Ctx c = onIsland();
            uSkyBlock usb = requireUsb();
            IslandInfo island = usb.getIslandInfo(c.playerInfo());
            check(island != null, "island record missing for the level-scan scenario");
            String islandName = island.getName();
            AtomicReference<Double> baseline = new AtomicReference<>();
            AtomicBoolean baselineDone = new AtomicBoolean();
            usb.calculateScoreAsync(c.player(), islandName, new Callback<us.talabrek.ultimateskyblock.api.model.IslandScore>() {
                @Override
                public void run() {
                    baseline.set(island.getLevel());
                    baselineDone.set(true);
                }
            });
            scenario.await(baselineDone::get, Duration.ofSeconds(30), () -> {
                Location origin = c.player().getLocation();
                List<Block> placed = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    Block block = origin.clone().add(0, 3, i).getBlock(); // above the player, in open air
                    block.setType(Material.DIAMOND_BLOCK);
                    placed.add(block);
                }
                AtomicBoolean rescanDone = new AtomicBoolean();
                usb.calculateScoreAsync(c.player(), islandName, new Callback<us.talabrek.ultimateskyblock.api.model.IslandScore>() {
                    @Override
                    public void run() {
                        rescanDone.set(true);
                    }
                });
                scenario.await(rescanDone::get, Duration.ofSeconds(30), () -> {
                    double after = island.getLevel();
                    placed.forEach(block -> block.setType(Material.AIR)); // leave the scan area clean for later scenarios
                    check(after > baseline.get(),
                        "island level did not rise after a real scan of placed diamond blocks (baseline="
                            + baseline.get() + ", after=" + after + ")");
                    scenario.pass("a real chunk-snapshot scan raised the island level after placing high-value blocks");
                }, "island level rescan did not complete before the deadline");
            }, "baseline island level scan did not complete before the deadline");
        }

        // Reward: experience is granted on completion.
        private void challengeXpReward(Scenario scenario) {
            Ctx c = onIsland();
            c.player().getInventory().clear();
            c.player().getInventory().addItem(new ItemStack(Material.STONE, 1));
            int experienceBefore = c.player().getTotalExperience();
            c.logic().completeChallenge(c.player(), XP);
            awaitCount(scenario, c, XP, 1, () -> {
                check(c.player().getTotalExperience() > experienceBefore, "experience reward should increase the player's total experience");
                scenario.pass("experience reward granted on completion");
            });
        }

        // Reward: a console command runs on completion (dispatched via the scheduler, so await its effect).
        private void challengeCommandReward(Scenario scenario) {
            Ctx c = onIsland();
            c.player().getInventory().clear();
            c.player().getInventory().addItem(new ItemStack(Material.STONE, 1));
            c.logic().completeChallenge(c.player(), COMMAND);
            awaitCount(scenario, c, COMMAND, 1, () ->
                scenario.await(() -> count(c.player(), Material.CAKE) == 1, Duration.ofSeconds(5),
                    () -> scenario.pass("command reward executed the console give on completion"),
                    "command reward did not deliver the expected item"));
        }

        // Reward: a biome unlock derived from completion state.
        private void challengeBiomeReward(Scenario scenario) {
            Ctx c = onIsland();
            uSkyBlock usb = requireUsb();
            IslandInfo island = usb.getIslandInfo(c.playerInfo());
            check(island != null, "island record missing for the biome reward challenge");
            IslandBiomeUnlocks unlocks = new IslandBiomeUnlocks(usb.getChallengeLogic(), usb.getRuntimeConfigs());
            check(!unlocks.isUnlocked(island, "jungle"), "jungle biome should be locked before the challenge is completed");
            c.player().getInventory().clear();
            c.player().getInventory().addItem(new ItemStack(Material.STONE, 1));
            c.logic().completeChallenge(c.player(), BIOME);
            awaitCount(scenario, c, BIOME, 1, () -> {
                check(unlocks.isUnlocked(island, "jungle"), "biome reward should unlock the jungle biome");
                scenario.pass("biome reward unlocked the configured biome");
            });
        }

        // Unlock gating: a challenge is unavailable until its prerequisite challenge is completed.
        private void challengeUnlockGated(Scenario scenario) {
            Ctx c = onIsland();
            c.player().getInventory().clear();
            c.player().getInventory().addItem(new ItemStack(Material.STONE, 1));
            c.logic().completeChallenge(c.player(), GATED);
            afterTicks(scenario, 3, () -> {
                check(c.logic().checkChallenge(c.playerInfo(), GATED) == 0, "gated challenge must be unavailable before its prerequisite");
                check(count(c.player(), Material.STONE) == 1, "locked challenge attempt must not consume items");
                c.player().getInventory().clear();
                c.player().getInventory().addItem(new ItemStack(Material.STONE, 1));
                c.logic().completeChallenge(c.player(), PREREQ);
                awaitCount(scenario, c, PREREQ, 1, () -> {
                    c.player().getInventory().clear();
                    c.player().getInventory().addItem(new ItemStack(Material.STONE, 1));
                    c.logic().completeChallenge(c.player(), GATED);
                    awaitCount(scenario, c, GATED, 1, () ->
                        scenario.pass("gated challenge unlocked only after its prerequisite was completed"));
                });
            });
        }

        // Admin completion bypasses every requirement (the fixture asks for an impossible hand-in) and
        // grants no reward.
        private void challengeAdminComplete(Scenario scenario) {
            Ctx c = onIsland();
            c.player().getInventory().clear(); // the fixture requires 64 netherite ingots a player never has
            int emeraldBefore = count(c.player(), Material.EMERALD);
            requireUsb().getChallengeLogic().completeChallengeForAdmin(c.playerInfo(), ADMIN,
                () -> { }, error -> scenario.fail(Verdict.Category.PLUGIN_FAIL, error));
            awaitCount(scenario, c, ADMIN, 1, () -> {
                check(count(c.player(), Material.EMERALD) == emeraldBefore, "admin-complete must not grant challenge rewards");
                scenario.pass("admin-complete bypassed requirements and granted no reward");
            });
        }

        private record Ctx(Player player, PlayerInfo playerInfo, ChallengeLogic logic) { }

        // Resolves the online fixture player, asserts it owns an island, and places it on that island.
        private Ctx onIsland() {
            Player player = Bukkit.getPlayerExact(PLAYER_NAME);
            check(player != null && player.isOnline(), "fixture player disconnected before challenge execution");
            uSkyBlock usb = requireUsb();
            PlayerInfo playerInfo = usb.getPlayerInfo(player);
            check(playerInfo != null && playerInfo.getHasIsland(), "fixture player has no island");
            Location home = playerInfo.getHomeLocation();
            check(validLocation(home) && player.teleport(home), "could not place player on its island");
            return new Ctx(player, playerInfo, usb.getChallengeLogic());
        }

        // Waits until a challenge reaches an expected completion count (completion persists asynchronously).
        private void awaitCount(Scenario scenario, Ctx c, ChallengeId id, int expected, Runnable onReached) {
            scenario.await(() -> c.logic().checkChallenge(c.playerInfo(), id) == expected, Duration.ofSeconds(15),
                onReached, "challenge " + id.value() + " did not reach completion count " + expected + " before the deadline");
        }

        // Runs assertions a few ticks later, once a rejected completion has had a chance to (not) settle.
        private void afterTicks(Scenario scenario, long ticks, Runnable body) {
            Bukkit.getScheduler().runTaskLater(IntegrationTestPlugin.this, () -> {
                try {
                    body.run();
                } catch (Throwable throwable) {
                    scenario.fail(Verdict.Category.PLUGIN_FAIL, throwable);
                }
            }, ticks);
        }

        // Arranges island level and warp in the fresh phase. Both persist to <island>.yml and reload
        // on boot, so the restart phase can prove they survive a real cross-process restart - a
        // concern a unit test cannot exercise.
        private void persistState(Scenario scenario) {
            Ctx c = onIsland();
            IslandInfo island = requireUsb().getIslandInfo(c.playerInfo());
            check(island != null, "island record is missing");
            Location warp = c.playerInfo().getHomeLocation();
            check(validLocation(warp), "home location is invalid");
            island.setLevel(PERSIST_LEVEL);
            island.setWarpLocation(warp);
            check(island.getLevel() == PERSIST_LEVEL, "island level was not applied before restart");
            check(island.hasWarp(), "warp was not activated before restart");
            Properties state = readState();
            state.setProperty("islandLevel", Double.toString(PERSIST_LEVEL));
            putLocation(state, "warp", warp);
            writeState(state);
            scenario.pass("island level and warp were arranged for the restart-persistence check");
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
                check(island.getLevel() == Double.parseDouble(requireProperty(state, "islandLevel")),
                    "island level did not survive restart");
                check(island.hasWarp(), "warp activation did not survive restart");
                check(sameBlock(island.getWarpLocation(), state, "warp"), "warp location did not survive restart");
                ChallengeId challengeId = ChallengeId.of(requireProperty(state, "challengeId"));
                int expected = integer(state, "challengeCompletions");
                check(usb.getChallengeLogic().checkChallenge(playerInfo, challengeId) == expected,
                    "challenge completion count did not survive restart");
                ChallengeId repeatableId = ChallengeId.of(requireProperty(state, "repeatableChallengeId"));
                int repeatableExpected = integer(state, "repeatableCompletions");
                check(usb.getChallengeLogic().checkChallenge(playerInfo, repeatableId) == repeatableExpected,
                    "repeatable challenge completion count did not survive restart");
                scenario.pass("island, owner, membership, home, region, marker, level, warp, and challenge progress survived restart");
            }, "uSkyBlock did not complete restart initialization before the deadline");
        }

        private void secondarySmokes(Scenario scenario) {
            uSkyBlock usb = requireUsb();
            ChallengeLogic logic = usb.getChallengeLogic();
            List<ChallengeId> ids = logic.getAllChallengeIds();
            check(!ids.isEmpty(), "no challenges were parsed");
            for (ChallengeId id : ids) check(logic.getDefinitionById(id).isPresent(), "challenge index could not resolve " + id.value());
            ChallengeDefinition fixture = logic.getDefinitionById(CHALLENGE_ID).orElseThrow();
            ItemStack display = fixture.display().displayItem().create(
                ChallengeText.plain(fixture.display().name()), ChallengeText.plain(fixture.display().description()));
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
