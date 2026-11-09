package us.talabrek.ultimateskyblock.update;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdatesTest {

    private static final Instant PUBLISHED = Instant.parse("2026-10-25T16:07:41.138344Z");
    private static final List<String> PAPER_SPIGOT = List.of("paper", "spigot");
    private static final ServerPlatform SPIGOT_26_3 = ServerPlatform.spigot("26.3");
    private static final ServerPlatform PAPER_26_3 = ServerPlatform.paper("26.3");

    private static ModrinthVersion version(String version, List<String> gameVersions) {
        return version(version, "release", gameVersions, PAPER_SPIGOT);
    }

    private static ModrinthVersion version(String version, String type, List<String> gameVersions,
                                           List<String> loaders) {
        return new ModrinthVersion(version, type, PUBLISHED, gameVersions, loaders, null);
    }

    private static Updates release(String currentVersion, ServerPlatform platform, ModrinthVersion... versions) {
        return Updates.of(List.of(versions), "RELEASE", currentVersion, platform);
    }

    private static String versionOf(ModrinthVersion version) {
        return version != null ? version.version() : null;
    }

    @Test
    public void newerCompatibleVersionIsTheUpdate() {
        Updates updates = release("3.7.1", SPIGOT_26_3,
            version("3.7.1", List.of("26.2", "26.3")), version("3.8.0", List.of("26.2", "26.3")));

        assertEquals("3.8.0", versionOf(updates.update()));
        assertEquals("3.8.0", versionOf(updates.latestOnMajor()));
        assertNull(updates.newMajor());
    }

    @Test
    public void versionForAnotherMinecraftVersionIsNotTheUpdate() {
        Updates updates = release("3.7.1", SPIGOT_26_3,
            version("3.7.1", List.of("26.3")), version("3.8.1", List.of("26.4")), version("3.8.0", List.of("26.3")));

        assertEquals("3.8.0", versionOf(updates.update()));
    }

    @Test
    public void noCompatibleVersionMeansNoUpdate() {
        Updates updates = release("3.7.1", SPIGOT_26_3, version("3.7.1", List.of("26.3")),
            version("3.8.0", List.of("26.4")));

        assertNull(updates.update());
        assertEquals("3.7.1", versionOf(updates.latestOnMajor()));
        assertEquals("3.8.0", versionOf(updates.newest()));
    }

    @Test
    public void versionForAnotherLoaderIsNotTheUpdate() {
        Updates onSpigot = release("3.7.1", SPIGOT_26_3,
            version("3.8.0", "release", List.of("26.3"), List.of("paper")),
            version("3.7.2", "release", List.of("26.3"), List.of("spigot")));
        Updates onPaper = release("3.7.1", PAPER_26_3,
            version("3.8.0", "release", List.of("26.3"), List.of("folia")),
            version("3.7.2", "release", List.of("26.3"), List.of("paper")));

        assertEquals("3.7.2", versionOf(onSpigot.update()));
        assertEquals("3.7.2", versionOf(onPaper.update()));
    }

    @Test
    public void paperRunsSpigotAndBukkitVersions() {
        ModrinthVersion spigotOnly = version("3.8.0", "release", List.of("26.3"), List.of("spigot"));
        ModrinthVersion bukkitOnly = version("3.8.0", "release", List.of("26.3"), List.of("bukkit"));
        ModrinthVersion paperOnly = version("3.8.0", "release", List.of("26.3"), List.of("paper"));

        assertTrue(PAPER_26_3.runs(spigotOnly));
        assertTrue(PAPER_26_3.runs(bukkitOnly));
        assertTrue(PAPER_26_3.runs(paperOnly));
        assertTrue(SPIGOT_26_3.runs(spigotOnly));
        assertTrue(SPIGOT_26_3.runs(bukkitOnly));
        assertFalse(SPIGOT_26_3.runs(paperOnly));
    }

    @Test
    public void newMajorVersionIsFoundWhetherItRunsHereOrNot() {
        Updates compatible = release("3.7.1", SPIGOT_26_3, version("4.0.0", List.of("26.3")),
            version("3.7.1", List.of("26.3")));
        Updates incompatible = release("3.7.1", SPIGOT_26_3, version("4.0.0", List.of("26.4")),
            version("3.7.1", List.of("26.3")));

        assertEquals("4.0.0", versionOf(compatible.newMajor()));
        assertEquals("4.0.0", versionOf(incompatible.newMajor()));
        assertNull(compatible.update());
        assertNull(incompatible.update());
        assertEquals("26.4", incompatible.newMajor().minecraftVersions());
    }

    @Test
    public void ownMajorUpdateAndNewMajorAreApart() {
        Updates updates = release("3.8.0-SNAPSHOT", SPIGOT_26_3, version("4.1.0", List.of("26.4")),
            version("4.0.0", List.of("26.3")), version("3.8.0", List.of("26.3")), version("3.7.1", List.of("26.3")));

        assertEquals("3.8.0", versionOf(updates.update()));
        assertEquals("4.1.0", versionOf(updates.newMajor()));
    }

    @Test
    public void versionsAreOrderedByVersionNotByListOrder() {
        Updates updates = release("3.7.0", SPIGOT_26_3, version("3.7.0", List.of("26.3")),
            version("3.10.0", List.of("26.3")), version("3.9.2", List.of("26.3")));

        assertEquals("3.10.0", versionOf(updates.update()));
    }

    @Test
    public void olderMajorVersionIsNotReported() {
        Updates updates = release("4.0.0-SNAPSHOT", SPIGOT_26_3, version("3.7.1", List.of("26.3")));

        assertNull(updates.update());
        assertNull(updates.newMajor());
        assertNull(updates.newest());
    }

    @Test
    public void snapshotOfNewMajorVersionGetsItsReleaseAsRegularUpdate() {
        Updates updates = release("4.0.0-SNAPSHOT", SPIGOT_26_3, version("4.0.0", List.of("26.3")),
            version("3.7.1", List.of("26.3")));

        assertEquals("4.0.0", versionOf(updates.update()));
        assertNull(updates.newMajor());
    }

    @Test
    public void releaseChannelIgnoresBetaAndAlpha() {
        List<ModrinthVersion> versions = List.of(
            version("4.0.0-beta.1", "beta", List.of("26.3"), PAPER_SPIGOT),
            version("3.8.1-alpha", "alpha", List.of("26.3"), PAPER_SPIGOT),
            version("3.8.0-beta", "beta", List.of("26.3"), PAPER_SPIGOT),
            version("3.7.1", List.of("26.3")));

        Updates release = Updates.of(versions, "RELEASE", "3.7.0", SPIGOT_26_3);

        assertEquals("3.7.1", versionOf(release.update()));
        assertNull(release.newMajor());
    }

    @Test
    public void stagingChannelReadsBetaAndAlpha() {
        List<ModrinthVersion> versions = List.of(
            version("4.0.0-beta.1", "beta", List.of("26.3"), PAPER_SPIGOT),
            version("3.8.1-alpha", "alpha", List.of("26.3"), PAPER_SPIGOT),
            version("3.7.1", List.of("26.3")));

        Updates staging = Updates.of(versions, "staging", "3.7.0", SPIGOT_26_3);

        assertEquals("3.8.1-alpha", versionOf(staging.update()));
        assertEquals("4.0.0-beta.1", versionOf(staging.newMajor()));
    }

    @Test
    public void versionsThatArentAVersionAreSkipped() {
        Updates updates = release("3.7.0", SPIGOT_26_3, version("nightly", List.of("26.3")),
            version("v4", List.of("26.3")), version("3.7.1", List.of("26.3")));

        assertEquals(List.of("3.7.1"), updates.newestFirst().stream().map(ModrinthVersion::version).toList());
    }

    @Test
    public void readsTheMinecraftVersionOfSpigotAndPaper() {
        assertEquals("26.3", ServerPlatform.minecraftVersion("26.3-R0.1-SNAPSHOT"));
        assertEquals("26.3", ServerPlatform.minecraftVersion("26.3.build.41-alpha"));
        assertEquals("1.21.11", ServerPlatform.minecraftVersion("1.21.11-R0.1-SNAPSHOT"));
    }
}
