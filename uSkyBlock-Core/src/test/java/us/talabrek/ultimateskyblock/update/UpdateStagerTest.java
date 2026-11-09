package us.talabrek.ultimateskyblock.update;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.test.MutableClock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateStagerTest {

    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");
    private static final Duration DELAY = Duration.ofDays(3);
    private static final List<String> PAPER_SPIGOT = List.of("paper", "spigot");
    private static final List<String> MC_26_3 = List.of("26.2", "26.3");

    @TempDir
    Path plugins;

    private final ModrinthStandIn modrinth;
    private final MutableClock clock = new MutableClock(NOW);
    private final List<LogRecord> logs = new ArrayList<>();
    private UpdateServer server;
    private UpdateStager stager;

    public UpdateStagerTest() throws IOException {
        modrinth = new ModrinthStandIn();
    }

    @AfterEach
    public void stopModrinth() {
        modrinth.close();
    }

    private UpdateStager stager() {
        if (stager == null) {
            server = new UpdateServer(plugins, "3.7.1");
            stager = new UpdateStager(server.plugin, modrinth.client(), clock, loggerInto(logs));
        }
        return stager;
    }

    private Updates updates() throws IOException {
        ModrinthClient client = modrinth.client();
        return Updates.of(ModrinthVersion.parseList(client.body(client.versionsUri())), "RELEASE", "3.7.1",
            ServerPlatform.spigot("26.3"));
    }

    private ModrinthVersion listed(String version) throws IOException {
        return updates().newestFirst().stream().filter(v -> v.version().equals(version)).findFirst().orElseThrow();
    }

    private UpdateStager.Result stage(String version) throws IOException {
        return stager().stage(listed(version), "Admin", false).join();
    }

    private List<Path> updateFolder() throws IOException {
        Path folder = plugins.resolve("update");
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.toList();
        }
    }

    private static Logger loggerInto(List<LogRecord> records) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }

    @Test
    public void stagesUnderTheRunningJarName() throws IOException {
        modrinth.version("3.8.0", "release", NOW, MC_26_3, PAPER_SPIGOT);

        UpdateStager.Result result = stage("3.8.0");

        assertNull(result.failure());
        assertEquals(List.of(server.staged()), updateFolder());
        assertEquals("uSkyBlock 3.8.0", Files.readString(server.staged()));
        assertEquals(new UpdateStager.StagedUpdate("3.8.0", false), stager.staged());
        assertNull(result.replaced());
        assertEquals(List.of("Staged uSkyBlock 3.8.0 as " + server.staged() + " for the next restart (by Admin)."),
            logs.stream().map(LogRecord::getMessage).toList());
    }

    @Test
    public void hashMismatchDeletesTheDownload() throws IOException {
        byte[] jar = "tampered".getBytes(StandardCharsets.UTF_8);
        modrinth.version("3.8.0", "release", NOW, MC_26_3, PAPER_SPIGOT, jar, ModrinthStandIn.sha512(new byte[0]));

        UpdateStager.Result result = stage("3.8.0");

        assertEquals(UpdateStager.Failure.HASH_MISMATCH, result.failure());
        assertEquals(List.of(), updateFolder());
        assertNull(stager.staged());
    }

    @Test
    public void failedDownloadStagesNothing() throws IOException {
        modrinth.version("3.8.0", "release", NOW, MC_26_3, PAPER_SPIGOT);
        ModrinthVersion version = listed("3.8.0");
        modrinth.answer(503, "");

        UpdateStager.Result result = stager().stage(version, "Admin", false).join();

        assertEquals(UpdateStager.Failure.UNREACHABLE, result.failure());
        assertEquals(List.of(), updateFolder());
    }

    @Test
    public void unwritableUpdateFolderIsRefused() throws IOException {
        modrinth.version("3.8.0", "release", NOW, MC_26_3, PAPER_SPIGOT);
        stager();
        Files.writeString(plugins.resolve("update"), "not a folder");

        UpdateStager.Result result = stage("3.8.0");

        assertEquals(UpdateStager.Failure.NOT_WRITABLE, result.failure());
        assertEquals(List.of(), modrinth.fileRequests());
    }

    @Test
    public void updateFolderThatIsThePluginsFolderIsRefused() throws IOException {
        modrinth.version("3.8.0", "release", NOW, MC_26_3, PAPER_SPIGOT);
        stager();
        server.updateFolder(plugins.toFile());

        assertEquals(UpdateStager.Failure.NO_UPDATE_FOLDER, stage("3.8.0").failure());
        server.updateFolder(null);
        assertEquals(UpdateStager.Failure.NO_UPDATE_FOLDER, stage("3.8.0").failure());
        assertEquals(List.of(), modrinth.fileRequests());
        assertFalse(Files.exists(plugins.resolve(UpdateServer.JAR_NAME)));
    }

    @Test
    public void secondStagingReplacesTheFirst() throws IOException {
        modrinth.version("3.8.0", "release", NOW, MC_26_3, PAPER_SPIGOT)
            .version("3.8.1", "release", NOW, MC_26_3, PAPER_SPIGOT);
        stage("3.8.0");

        UpdateStager.Result result = stage("3.8.1");

        assertEquals("3.8.0", result.replaced());
        assertEquals("uSkyBlock 3.8.1", Files.readString(server.staged()));
        assertEquals(List.of(server.staged()), updateFolder());
    }

    @Test
    public void startupReadsTheStagedVersion() throws IOException {
        stager();
        Files.createDirectories(server.staged().getParent());
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(server.staged()))) {
            jar.putNextEntry(new JarEntry("plugin.yml"));
            jar.write("name: uSkyBlock\nversion: 3.8.1\nmain: us.talabrek.ultimateskyblock.uSkyBlock\n"
                .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        stager.startup();

        assertEquals(new UpdateStager.StagedUpdate("3.8.1", false), stager.staged());
    }

    @Test
    public void startupWithoutStagedJarKnowsNoStagedVersion() {
        stager().startup();

        assertNull(stager.staged());
    }

    @Test
    public void autoStageStagesAnUpdateOlderThanTheDelay() throws IOException {
        modrinth.version("3.8.0", "release", NOW.minus(DELAY), MC_26_3, PAPER_SPIGOT);

        UpdateStager.Result result = stager().autoStage(updates(), DELAY).join();

        assertNull(result.failure());
        assertEquals(new UpdateStager.StagedUpdate("3.8.0", true), stager.staged());
        assertTrue(logs.getFirst().getMessage().endsWith("(by auto-stage)."));
    }

    @Test
    public void autoStageWaitsForAYoungerUpdate() throws IOException {
        modrinth.version("3.8.0", "release", NOW.minus(DELAY).plusSeconds(1), MC_26_3, PAPER_SPIGOT);

        assertNull(stager().autoStage(updates(), DELAY).join());
        assertEquals(List.of(), updateFolder());

        clock.advance(Duration.ofSeconds(1));
        assertNull(stager.autoStage(updates(), DELAY).join().failure());
    }

    @Test
    public void autoStageWaitsWhileTheNewestUpdateIsYoung() throws IOException {
        modrinth.version("3.8.1", "release", NOW.minus(Duration.ofDays(1)), MC_26_3, PAPER_SPIGOT)
            .version("3.8.0", "release", NOW.minus(Duration.ofDays(10)), MC_26_3, PAPER_SPIGOT);

        assertNull(stager().autoStage(updates(), DELAY).join());
        assertEquals(List.of(), modrinth.fileRequests());
    }

    @Test
    public void autoStageNeverStagesANewMajorVersion() throws IOException {
        modrinth.version("4.0.0", "release", NOW.minus(Duration.ofDays(30)), MC_26_3, PAPER_SPIGOT);

        assertNull(stager().autoStage(updates(), DELAY).join());
        assertEquals(List.of(), modrinth.fileRequests());
    }

    @Test
    public void autoStageNeverStagesAnIncompatibleUpdate() throws IOException {
        modrinth.version("3.8.1", "release", NOW.minus(Duration.ofDays(30)), List.of("26.4"), PAPER_SPIGOT)
            .version("3.8.0", "release", NOW.minus(Duration.ofDays(30)), MC_26_3, List.of("paper"));

        assertNull(stager().autoStage(updates(), DELAY).join());
        assertEquals(List.of(), modrinth.fileRequests());
    }

    @Test
    public void autoStageDoesNotStageTheSameVersionTwice() throws IOException {
        modrinth.version("3.8.0", "release", NOW.minus(DELAY), MC_26_3, PAPER_SPIGOT);
        stager().autoStage(updates(), DELAY).join();

        assertNull(stager.autoStage(updates(), DELAY).join());
        assertEquals(1, modrinth.fileRequests().size());
    }

    @Test
    public void autoStageReplacesAnOlderStagedVersion() throws IOException {
        modrinth.version("3.8.0", "release", NOW.minus(Duration.ofDays(10)), MC_26_3, PAPER_SPIGOT);
        stage("3.8.0");
        modrinth.version("3.8.1", "release", NOW.minus(DELAY), MC_26_3, PAPER_SPIGOT);

        UpdateStager.Result result = stager.autoStage(updates(), DELAY).join();

        assertEquals("3.8.0", result.replaced());
        assertEquals(new UpdateStager.StagedUpdate("3.8.1", true), stager.staged());
    }

    @Test
    public void autoStageKeepsANewerStagedVersion() throws IOException {
        modrinth.version("4.0.0", "release", NOW.minus(Duration.ofDays(10)), MC_26_3, PAPER_SPIGOT);
        stage("4.0.0");
        modrinth.version("3.8.1", "release", NOW.minus(DELAY), MC_26_3, PAPER_SPIGOT);

        assertNull(stager.autoStage(updates(), DELAY).join());
        assertEquals(new UpdateStager.StagedUpdate("4.0.0", false), stager.staged());
        assertEquals("uSkyBlock 4.0.0", Files.readString(server.staged()));
        assertEquals(1, modrinth.fileRequests().size());
    }
}
