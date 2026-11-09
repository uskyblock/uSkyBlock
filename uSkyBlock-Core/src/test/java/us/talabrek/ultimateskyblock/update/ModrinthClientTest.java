package us.talabrek.ultimateskyblock.update;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.test.MutableClock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ModrinthClientTest {

    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");

    private final ModrinthStandIn modrinth;

    public ModrinthClientTest() throws IOException {
        modrinth = new ModrinthStandIn();
    }

    @AfterEach
    public void stopModrinth() {
        modrinth.close();
    }

    @Test
    public void listsTheProjectsVersionsWithoutChangelogs() throws IOException {
        modrinth.version("3.8.0", "release", NOW, List.of("26.3"), List.of("paper", "spigot"));
        ModrinthClient client = modrinth.client();

        String body = client.body(client.versionsUri());

        assertEquals(List.of(ModrinthStandIn.VERSIONS_PATH + "?include_changelog=false"), modrinth.requests());
        assertEquals("3.8.0", ModrinthVersion.parseList(body).getFirst().version());
    }

    @Test
    public void sendsAUserAgentWithVersionAndContact() throws IOException {
        ModrinthClient client = modrinth.client();

        client.body(client.versionsUri());

        assertEquals(List.of("uskyblock/uSkyBlock/3.7.1 (https://github.com/uskyblock/uSkyBlock)"), modrinth.userAgents());
    }

    @Test
    public void downloadsAFile() throws IOException {
        modrinth.version("3.8.0", "release", NOW, List.of("26.3"), List.of("paper"));
        ModrinthClient client = modrinth.client();
        ModrinthVersion version = ModrinthVersion.parseList(client.body(client.versionsUri())).getFirst();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        client.download(version.file().url(), out);

        assertEquals("uSkyBlock 3.8.0", out.toString());
    }

    @Test
    public void errorStatusIsAFailure() {
        modrinth.answer(500, "{\"error\": \"internal\"}");
        ModrinthClient client = modrinth.client();

        assertThrows(ModrinthClient.UnexpectedResponseException.class, () -> client.body(client.versionsUri()));
    }

    @Test
    public void unreachableModrinthIsAFailure() {
        ModrinthClient client = modrinth.client();
        modrinth.close();

        assertThrows(IOException.class, () -> client.body(client.versionsUri()));
    }

    @Test
    public void waitsForTheRateLimitToReset() throws IOException {
        MutableClock clock = new MutableClock(NOW);
        ModrinthClient client = modrinth.client(clock);
        modrinth.rateLimited("30");

        assertThrows(ModrinthClient.UnexpectedResponseException.class, () -> client.body(client.versionsUri()));
        modrinth.answer(200, "[]");
        clock.advance(Duration.ofSeconds(29));
        assertThrows(ModrinthClient.UnexpectedResponseException.class, () -> client.body(client.versionsUri()));
        assertEquals(1, modrinth.requests().size());

        clock.advance(Duration.ofSeconds(1));
        assertEquals("[]", client.body(client.versionsUri()));
        assertEquals(2, modrinth.requests().size());
    }

    @Test
    public void rateLimitWithoutResetWaitsAMinute() throws IOException {
        MutableClock clock = new MutableClock(NOW);
        ModrinthClient client = modrinth.client(clock);
        modrinth.rateLimited("soon");

        assertThrows(ModrinthClient.UnexpectedResponseException.class, () -> client.body(client.versionsUri()));
        modrinth.answer(200, "[]");
        clock.advance(Duration.ofSeconds(59));
        assertThrows(ModrinthClient.UnexpectedResponseException.class, () -> client.body(client.versionsUri()));
        assertEquals(1, modrinth.requests().size());

        clock.advance(Duration.ofSeconds(1));
        assertEquals("[]", client.body(client.versionsUri()));
        assertEquals(2, modrinth.requests().size());
    }
}
