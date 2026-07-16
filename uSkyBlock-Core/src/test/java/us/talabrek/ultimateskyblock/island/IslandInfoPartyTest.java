package us.talabrek.ultimateskyblock.island;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.player.IslandPerk;
import us.talabrek.ultimateskyblock.player.Perk;
import us.talabrek.ultimateskyblock.player.PerkLogic;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the config-backed party/membership bookkeeping on a real {@link IslandInfo}.
 *
 * <p>Note: {@link IslandInfo#addMember} and {@link IslandInfo#removeMember} are intentionally not
 * exercised here. Both unconditionally call {@code WorldGuardHandler.updateRegion(this)}, which
 * dereferences WorldEdit/WorldGuard types (e.g. {@code BlockVector3}, {@code ProtectedCuboidRegion})
 * that are {@code compileOnly} and absent from the test classpath. Invoking them raises a
 * {@link NoClassDefFoundError} that escapes the handler's {@code catch (Exception)}. The membership
 * bookkeeping those methods write is instead verified from the read side by seeding
 * {@code party.members} directly; the write paths belong to the live-server harness.
 */
public class IslandInfoPartyTest {
    private static final UUID LEADER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ALICE_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOB_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TARGET_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @TempDir
    Path tempDir;

    private uSkyBlock plugin;
    private RuntimeConfigs runtimeConfigs;
    private PlayerDB playerDB;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);

        plugin = mock(uSkyBlock.class);

        playerDB = mock(PlayerDB.class);
        doReturn(playerDB).when(plugin).getPlayerDB();

        // A real RuntimeConfig - the nested records cannot be mocked (ClassFormatError). Some methods
        // under test (e.g. getMaxPartySize -> getSchematicName) read the runtime config eagerly.
        runtimeConfigs = mock(RuntimeConfigs.class);
        doReturn(new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(buildConfig()))
            .when(runtimeConfigs).current();

        // Event firing (ban/unban/trust/untrust) goes through plugin.getServer().getPluginManager().callEvent.
        // A mock PluginManager makes callEvent a no-op, so the events are never cancelled.
        PluginManager pluginManager = mock(PluginManager.class);
        Server server = mock(Server.class);
        doReturn(pluginManager).when(server).getPluginManager();
        doReturn(server).when(plugin).getServer();

        // banPlayerInfo/unbanPlayerInfo look the PlayerInfo up via PlayerLogic; a mock returns null -> no-op.
        doReturn(mock(PlayerLogic.class)).when(plugin).getPlayerLogic();
    }

    @Test
    public void getMembersReturnsSeededMemberNames() throws Exception {
        doReturn("Leader").when(playerDB).getName(LEADER_UUID);
        doReturn("Alice").when(playerDB).getName(ALICE_UUID);

        IslandInfo island = createIsland("island", config -> {
            seedLeader(config);
            seedMember(config, ALICE_UUID, "Alice");
            config.set("party.currentSize", 2);
        });

        assertThat(island.getMembers(), containsInAnyOrder("Leader", "Alice"));
        assertThat(island.getMemberUUIDs(), containsInAnyOrder(LEADER_UUID, ALICE_UUID));
        assertEquals(2, island.getPartySize());
    }

    @Test
    public void partySizeAndMembersReflectPartyGrowth() throws Exception {
        doReturn("Leader").when(playerDB).getName(LEADER_UUID);
        doReturn("Alice").when(playerDB).getName(ALICE_UUID);
        doReturn("Bob").when(playerDB).getName(BOB_UUID);

        IslandInfo solo = createIsland("solo", config -> {
            seedLeader(config);
            config.set("party.currentSize", 1);
        });
        IslandInfo party = createIsland("party", config -> {
            seedLeader(config);
            seedMember(config, ALICE_UUID, "Alice");
            seedMember(config, BOB_UUID, "Bob");
            config.set("party.currentSize", 3);
        });

        assertEquals(1, solo.getPartySize());
        assertThat(solo.getMembers(), hasSize(1));
        assertEquals(3, party.getPartySize());
        assertThat(party.getMembers(), hasSize(3));
    }

    @Test
    public void isMemberIdentifiesSeededMembers() throws Exception {
        IslandInfo island = createIsland("island", config -> {
            seedLeader(config);
            seedMember(config, ALICE_UUID, "Alice");
            config.set("party.currentSize", 2);
        });

        OfflinePlayer alice = offlinePlayer(ALICE_UUID);
        OfflinePlayer stranger = offlinePlayer(TARGET_UUID);

        assertTrue(island.isMember(alice));
        assertFalse(island.isMember(stranger));
    }

    @Test
    public void isLeaderIdentifiesTheLeader() throws Exception {
        IslandInfo island = createIsland("island", config -> {
            seedLeader(config);
            seedMember(config, ALICE_UUID, "Alice");
            config.set("party.currentSize", 2);
        });

        OfflinePlayer leader = offlinePlayer(LEADER_UUID);
        OfflinePlayer member = offlinePlayer(ALICE_UUID);

        assertTrue(island.isLeader(leader));
        assertFalse(island.isLeader(member));
    }

    @Test
    public void getMaxPartySizeReturnsPerkDefaultWithoutOverride() throws Exception {
        stubPerkDefaultMaxPartySize(4);

        IslandInfo island = createIsland("island", config -> {
            seedLeader(config);
            config.set("party.currentSize", 1);
        });

        assertEquals(4, island.getMaxPartySize());
    }

    @Test
    public void getMaxPartySizeHonorsPerIslandOverride() throws Exception {
        stubPerkDefaultMaxPartySize(4);

        IslandInfo island = createIsland("island", config -> {
            seedLeader(config);
            seedMember(config, ALICE_UUID, "Alice");
            config.set("party.members." + ALICE_UUID + ".maxPartySizePermission", 8);
            config.set("party.currentSize", 2);
        });

        assertEquals(8, island.getMaxPartySize());
    }

    @Test
    public void banPlayerAddsBanAndUnbanRemovesIt() throws Exception {
        doReturn("Target").when(playerDB).getName(TARGET_UUID);
        OfflinePlayer target = offlinePlayer(TARGET_UUID);

        IslandInfo island = createIsland("island", config -> {
            seedLeader(config);
            config.set("party.currentSize", 1);
        });

        assertTrue(island.banPlayer(target, null));
        assertTrue(island.isBanned(target));
        assertThat(island.getBans(), contains("Target"));

        assertTrue(island.unbanPlayer(target, null));
        assertFalse(island.isBanned(target));
        assertThat(island.getBans(), empty());
    }

    @Test
    public void banPlayerRejectsExistingMember() throws Exception {
        OfflinePlayer alice = offlinePlayer(ALICE_UUID);

        IslandInfo island = createIsland("island", config -> {
            seedLeader(config);
            seedMember(config, ALICE_UUID, "Alice");
            config.set("party.currentSize", 2);
        });

        assertFalse(island.banPlayer(alice, null));
        assertFalse(island.isBanned(alice));
    }

    @Test
    public void trustPlayerAddsTrusteeAndUntrustRemovesIt() throws Exception {
        doReturn("Target").when(playerDB).getName(TARGET_UUID);
        OfflinePlayer target = offlinePlayer(TARGET_UUID);

        IslandInfo island = createIsland("island", config -> {
            seedLeader(config);
            config.set("party.currentSize", 1);
        });

        assertTrue(island.trustPlayer(target, null));
        assertTrue(island.isTrusted(target));
        assertThat(island.getTrustees(), contains("Target"));

        assertTrue(island.untrustPlayer(target, null));
        assertFalse(island.isTrusted(target));
        assertThat(island.getTrustees(), empty());
    }

    private void stubPerkDefaultMaxPartySize(int maxPartySize) {
        PerkLogic perkLogic = mock(PerkLogic.class);
        IslandPerk islandPerk = mock(IslandPerk.class);
        Perk perk = mock(Perk.class);
        doReturn(perkLogic).when(plugin).getPerkLogic();
        doReturn(islandPerk).when(perkLogic).getIslandPerk(anyString());
        doReturn(perk).when(islandPerk).getPerk();
        doReturn(maxPartySize).when(perk).getMaxPartySize();
    }

    private OfflinePlayer offlinePlayer(UUID uuid) {
        OfflinePlayer player = mock(OfflinePlayer.class);
        doReturn(uuid).when(player).getUniqueId();
        return player;
    }

    private static void seedLeader(YamlConfiguration config) {
        config.set("party.leader", "Leader");
        config.set("party.leader-uuid", LEADER_UUID.toString());
        seedMember(config, LEADER_UUID, "Leader");
    }

    private static void seedMember(YamlConfiguration config, UUID uuid, String name) {
        config.set("party.members." + uuid + ".name", name);
    }

    private IslandInfo createIsland(String name, Consumer<YamlConfiguration> customizer) throws Exception {
        File islandDir = tempDir.resolve("islands").toFile();
        islandDir.mkdirs();
        File islandConfigFile = new File(islandDir, name + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", 3);
        customizer.accept(config);
        config.save(islandConfigFile);
        return new IslandInfo(name, plugin, runtimeConfigs, islandDir.toPath());
    }

    private static YamlConfiguration buildConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("language", "en");
        config.set("options.general.worldName", "skyworld");
        config.set("options.general.cooldownRestart", "1m");
        config.set("options.general.biomeChange", "1m");
        config.set("options.general.defaultBiome", "plains");
        config.set("options.general.defaultNetherBiome", "nether_wastes");
        config.set("options.advanced.confirmTimeout", "30s");
        config.set("options.advanced.playerdb.storage", "file");
        config.set("options.advanced.playerdb.nameCache", "maximumSize=100");
        config.set("options.advanced.playerdb.uuidCache", "maximumSize=100");
        config.set("options.advanced.playerCache", "maximumSize=100");
        config.set("options.advanced.islandCache", "maximumSize=100");
        config.set("options.advanced.island.saveEvery", 60);
        config.set("options.advanced.player.saveEvery", 60);
        config.set("options.party.invite-timeout", "1m");
        config.set("options.island.distance", 110);
        config.set("options.island.height", 120);
        config.set("options.island.topTenTimeout", "15m");
        config.set("options.island.islandTeleportDelay", "2s");
        config.set("options.island.chat-format", "default");
        config.set("options.party.chat-format", "default");
        config.set("nether.chunk-generator", "default");
        config.set("plugin-updates.branch", "LATEST");
        return config;
    }
}
