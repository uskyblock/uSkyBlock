package us.talabrek.ultimateskyblock.update;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ModrinthVersionTest {

    // Trimmed GET /v2/project/uskyblock/version response.
    private static final String VERSIONS = """
        [
          {
            "game_versions": ["1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2", "26.3"],
            "loaders": ["paper", "spigot"],
            "id": "UElJRSCm",
            "project_id": "KmR5sN3n",
            "name": "3.7.1",
            "version_number": "3.7.1",
            "date_published": "2026-10-25T16:07:41.138344Z",
            "version_type": "release",
            "status": "listed",
            "files": [
              {
                "hashes": {
                  "sha1": "911612f0d49be1e06bfd007c9ca7a4923d07ab03",
                  "sha512": "643b5195fd2646c357b8afcb0be20349c21018d98ed08b2e15cd7195e073298059a61a0043faee4c0997d3253e26c8a62e479284516d5b186720a49ffa53a053"
                },
                "url": "https://cdn.modrinth.com/data/KmR5sN3n/versions/UElJRSCm/uSkyBlock-3.7.1.jar",
                "filename": "uSkyBlock-3.7.1.jar",
                "primary": true,
                "size": 2464733
              }
            ],
            "dependencies": []
          }
        ]
        """;

    private static String withFiles(String files) {
        return """
            [{"version_number": "3.8.0", "version_type": "release", "date_published": "2026-11-01T10:00:00Z",
              "game_versions": ["26.3"], "loaders": ["paper"], "files": %s}]
            """.formatted(files);
    }

    private static String file(String name, Boolean primary) {
        return """
            {"url": "https://cdn.modrinth.com/%s", "filename": "%s", %s "hashes": {"sha512": "ab%s"}}
            """.formatted(name, name, primary != null ? "\"primary\": " + primary + "," : "", name.length());
    }

    @Test
    public void readsModrinthVersions() {
        ModrinthVersion version = ModrinthVersion.parseList(VERSIONS).getFirst();

        assertEquals("3.7.1", version.version());
        assertEquals("release", version.type());
        assertEquals(Instant.parse("2026-10-25T16:07:41.138344Z"), version.published());
        assertEquals(List.of("1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2", "26.3"), version.gameVersions());
        assertEquals(List.of("paper", "spigot"), version.loaders());
        assertEquals(new ModrinthVersion.DownloadFile(
            URI.create("https://cdn.modrinth.com/data/KmR5sN3n/versions/UElJRSCm/uSkyBlock-3.7.1.jar"),
            "uSkyBlock-3.7.1.jar",
            "643b5195fd2646c357b8afcb0be20349c21018d98ed08b2e15cd7195e073298059a61a0043faee4c0997d3253e26c8a62e479284516d5b186720a49ffa53a053"),
            version.file());
        assertEquals(URI.create("https://modrinth.com/plugin/uskyblock/version/3.7.1"), version.page());
        assertEquals("Paper/Spigot", version.platforms());
    }

    @Test
    public void picksThePrimaryFile() {
        ModrinthVersion version = ModrinthVersion.parseList(withFiles(
            "[" + file("sources.jar", false) + "," + file("uSkyBlock.jar", true) + "]")).getFirst();

        assertEquals("uSkyBlock.jar", version.file().filename());
    }

    @Test
    public void takesTheFirstFileWhenNoneIsPrimary() {
        ModrinthVersion version = ModrinthVersion.parseList(withFiles(
            "[" + file("first.jar", null) + "," + file("second.jar", false) + "]")).getFirst();

        assertEquals("first.jar", version.file().filename());
    }

    @Test
    public void versionWithoutFilesHasNoFile() {
        assertNull(ModrinthVersion.parseList(withFiles("[]")).getFirst().file());
        assertNull(ModrinthVersion.parseList(withFiles("[{\"url\": \"https://cdn.modrinth.com/x.jar\"}]"))
            .getFirst().file());
    }

    @Test
    public void entriesMissingAFieldAreSkipped() {
        String body = """
            [null, "3.8.0", {"version_number": "3.8.0"},
             {"version_number": "3.8.0", "version_type": "release", "date_published": "yesterday",
              "game_versions": [], "loaders": []},
             {"version_number": "3.8.1", "version_type": "release", "date_published": "2026-11-01T10:00:00Z",
              "game_versions": "26.3", "loaders": []},
             {"version_number": "3.8.2", "version_type": "release", "date_published": "2026-11-01T10:00:00Z",
              "game_versions": ["26.3"], "loaders": ["paper"]}]
            """;

        assertEquals(List.of("3.8.2"), ModrinthVersion.parseList(body).stream().map(ModrinthVersion::version).toList());
    }

    @Test
    public void malformedBodyIsNoList() {
        assertNull(ModrinthVersion.parseList("[{\"version_number\": "));
        assertNull(ModrinthVersion.parseList("<html><body>Bad gateway</body></html>"));
        assertNull(ModrinthVersion.parseList("{\"error\": \"not_found\"}"));
        assertNull(ModrinthVersion.parseList(""));
        assertNull(ModrinthVersion.parseList(null));
    }

    @Test
    public void emptyListIsNoFailure() {
        assertEquals(List.of(), ModrinthVersion.parseList("[]"));
    }
}
