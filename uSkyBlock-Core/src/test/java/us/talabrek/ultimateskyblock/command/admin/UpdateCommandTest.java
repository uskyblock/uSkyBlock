package us.talabrek.ultimateskyblock.command.admin;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.SkyUpdateChecker;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.handler.ConfirmHandler;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.update.ModrinthClient;
import us.talabrek.ultimateskyblock.update.ModrinthStandIn;
import us.talabrek.ultimateskyblock.update.ServerPlatform;
import us.talabrek.ultimateskyblock.update.UpdateServer;
import us.talabrek.ultimateskyblock.update.UpdateStager;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateCommandTest {

    private static final Instant PUBLISHED = Instant.parse("2026-10-25T16:07:41Z");
    private static final List<String> PAPER_SPIGOT = List.of("paper", "spigot");
    private static final List<String> MC_26_3 = List.of("26.2", "26.3");
    private static final String CHECKING = "Checking Modrinth for a newer uSkyBlock...";

    @TempDir
    Path plugins;

    private final ModrinthStandIn modrinth;
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final ConfirmHandler confirmHandler = mock(ConfirmHandler.class);
    private final CommandSender admin = mock(CommandSender.class);
    private UpdateServer server;
    private UpdateCommand command;

    public UpdateCommandTest() throws IOException {
        modrinth = new ModrinthStandIn();
    }

    @BeforeEach
    public void setUp() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Msg.configure((sender, message) -> messages.add(Msg.plainText(message)));
        when(admin.getName()).thenReturn("Admin");
        server = new UpdateServer(plugins, "3.7.1");
        RuntimeConfigs runtimeConfigs = UpdateServer.config(true, "RELEASE", false);
        Logger logger = Logger.getLogger("UpdateCommandTest");
        ModrinthClient client = modrinth.client();
        UpdateStager stager = new UpdateStager(server.plugin, client, Clock.systemUTC(), logger);
        SkyUpdateChecker checker = new SkyUpdateChecker(server.plugin, runtimeConfigs, logger, client, stager,
            ServerPlatform.spigot("26.3"));
        command = new UpdateCommand(checker, stager, confirmHandler, server.scheduler);
    }

    @AfterEach
    public void tearDown() {
        Msg.configure(null);
        modrinth.close();
    }

    private void run() throws InterruptedException {
        command.execute(admin, "update", Map.of());
        server.runNextTask();
    }

    private void runAndStage() throws InterruptedException {
        run();
        server.runNextTask();
    }

    @Test
    public void regularUpdateIsStagedUnderTheRunningJarNameWithoutConfirmation() throws Exception {
        modrinth.version("3.7.1", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT)
            .version("3.8.0", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT);

        runAndStage();

        assertEquals(List.of(CHECKING, "Downloading uSkyBlock 3.8.0...",
            "uSkyBlock 3.8.0 is staged and takes effect at the next restart."), messages);
        assertEquals("uSkyBlock 3.8.0", Files.readString(server.staged()));
        assertEquals("uSkyBlock-3.7.1.jar", server.staged().getFileName().toString());
        verify(confirmHandler, never()).checkCommand(any(), anyString());
    }

    @Test
    public void regularUpdateComesBeforeACompatibleNewMajorVersion() throws Exception {
        modrinth.version("4.0.0", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT)
            .version("3.8.0", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT);

        runAndStage();

        assertEquals("uSkyBlock 3.8.0", Files.readString(server.staged()));
        verify(confirmHandler, never()).checkCommand(any(), anyString());
    }

    @Test
    public void secondRunReplacesTheStagedVersion() throws Exception {
        modrinth.version("3.8.0", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT);
        runAndStage();
        modrinth.version("3.8.1", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT);
        messages.clear();

        runAndStage();

        assertEquals("Replaced the staged uSkyBlock 3.8.0 with 3.8.1. It takes effect at the next restart.",
            messages.getLast());
        assertEquals("uSkyBlock 3.8.1", Files.readString(server.staged()));
    }

    @Test
    public void updateForAnotherMinecraftVersionIsRefused() throws Exception {
        modrinth.version("3.8.0", "release", PUBLISHED, List.of("26.4"), PAPER_SPIGOT);

        run();

        assertEquals(List.of(CHECKING, "uSkyBlock 3.8.0 needs Minecraft 26.4 on Paper/Spigot, but this server runs "
            + "Minecraft 26.3 on Spigot. Nothing was downloaded."), messages);
        assertEquals(List.of(), modrinth.fileRequests());
        assertTrue(server.hasNoTasks());
    }

    @Test
    public void updateForAnotherLoaderIsRefused() throws Exception {
        modrinth.version("3.8.0", "release", PUBLISHED, MC_26_3, List.of("paper"));

        run();

        assertEquals(List.of(CHECKING, "uSkyBlock 3.8.0 needs Minecraft 26.2, 26.3 on Paper, but this server runs "
            + "Minecraft 26.3 on Spigot. Nothing was downloaded."), messages);
        assertEquals(List.of(), modrinth.fileRequests());
    }

    @Test
    public void incompatibleNewMajorVersionIsRefused() throws Exception {
        modrinth.version("4.0.0", "release", PUBLISHED, List.of("26.4"), List.of("paper"));

        run();

        assertEquals(List.of(CHECKING, "uSkyBlock 4.0.0 needs Minecraft 26.4 on Paper, but this server runs "
            + "Minecraft 26.3 on Spigot. Nothing was downloaded."), messages);
        assertEquals(List.of(), modrinth.fileRequests());
        verify(confirmHandler, never()).checkCommand(any(), anyString());
    }

    @Test
    public void compatibleNewMajorVersionAsksFirstAndStagesOnlyAfterConfirming() throws Exception {
        modrinth.version("3.7.1", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT)
            .version("4.0.0", "release", PUBLISHED, List.of("26.3"), PAPER_SPIGOT);
        when(confirmHandler.checkCommand(admin, "/usb update")).thenReturn(false, true);

        run();

        assertEquals(List.of(CHECKING,
            "uSkyBlock 4.0.0 is a new major version. It changes your data and config at its first start, and needs "
                + "Minecraft 26.3 on Paper/Spigot.",
            "Back up your server first, and read the upgrade guide: https://uskyblock.github.io/uSkyBlock/admin/upgrading/",
            "Release notes: https://modrinth.com/plugin/uskyblock/version/4.0.0"), messages);
        assertEquals(List.of(), modrinth.fileRequests());
        assertFalse(Files.exists(server.staged()));

        messages.clear();
        runAndStage();

        assertEquals("uSkyBlock 4.0.0 is staged and takes effect at the next restart.", messages.getLast());
        assertEquals("uSkyBlock 4.0.0", Files.readString(server.staged()));
    }

    @Test
    public void hashMismatchIsRefused() throws Exception {
        byte[] jar = "tampered".getBytes(StandardCharsets.UTF_8);
        modrinth.version("3.8.0", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT, jar, ModrinthStandIn.sha512(new byte[0]));

        runAndStage();

        assertEquals("Could not stage uSkyBlock 3.8.0: the download doesn't match its SHA-512 hash on Modrinth, "
            + "so it was deleted.", messages.getLast());
        assertFalse(Files.exists(server.staged()));
    }

    @Test
    public void upToDate() throws Exception {
        modrinth.version("3.7.1", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT)
            .version("3.7.0", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT);

        run();

        assertEquals(List.of(CHECKING, "uSkyBlock 3.7.1 is up to date."), messages);
        assertTrue(server.hasNoTasks());
    }

    @Test
    public void unwritableUpdateFolderIsRefused() throws Exception {
        modrinth.version("3.8.0", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT);
        Files.writeString(plugins.resolve("update"), "not a folder");

        runAndStage();

        assertEquals("Could not stage uSkyBlock 3.8.0: " + plugins.resolve("update") + " can't be written.",
            messages.getLast());
    }

    @Test
    public void missingUpdateFolderIsRefused() throws Exception {
        modrinth.version("3.8.0", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT);
        server.updateFolder(plugins.toFile());

        runAndStage();

        assertEquals("Could not stage uSkyBlock 3.8.0: the server has no update folder apart from the plugins "
            + "folder. Set settings.update-folder in bukkit.yml.", messages.getLast());
        assertEquals(List.of(), modrinth.fileRequests());
    }

    @Test
    public void unreachableModrinthIsRefused() throws Exception {
        modrinth.close();

        run();

        assertEquals(List.of(CHECKING,
            "Could not reach Modrinth. Check for updates manually at https://modrinth.com/plugin/uskyblock"), messages);
    }

    @Test
    public void needsTheUpdatePermission() throws Exception {
        assertEquals("usb.admin.update", command.getPermission());

        YamlConfiguration pluginYml = new YamlConfiguration();
        pluginYml.options().pathSeparator('/');
        try (var reader = new InputStreamReader(Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream("plugin.yml")), StandardCharsets.UTF_8)) {
            pluginYml.load(reader);
        }
        assertTrue(pluginYml.getBoolean("permissions/usb.admin/children/usb.admin.update"));
        assertTrue(pluginYml.getBoolean("permissions/usb.admin.*/children/usb.admin.update"));
    }
}
