package us.talabrek.ultimateskyblock.update;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The uSkyBlock versions on the update channel, as seen from the running plugin and server.
 *
 * @param newestFirst The versions on the channel, newest first.
 */
public record Updates(
    @NotNull String currentVersion,
    @NotNull ServerPlatform platform,
    @NotNull List<ModrinthVersion> newestFirst
) {
    // A version starts with major.minor, e.g. 3.7.1 or 3.8.0-SNAPSHOT.
    private static final Pattern VERSION = Pattern.compile("(\\d{1,9})\\.\\d+.*");

    /**
     * The {@code version_type}s each {@code plugin-updates.branch} reads. STAGING was the latest build of any kind,
     * so it reads Modrinth's beta and alpha versions too.
     */
    private static final Set<String> RELEASE_TYPES = Set.of("release");
    private static final Set<String> STAGING_TYPES = Set.of("release", "beta", "alpha");

    /**
     * @param branch The {@code plugin-updates.branch}: RELEASE or STAGING.
     */
    public static @NotNull Updates of(
        @NotNull List<ModrinthVersion> versions,
        @NotNull String branch,
        @NotNull String currentVersion,
        @NotNull ServerPlatform platform
    ) {
        Set<String> types = branch.equalsIgnoreCase("STAGING") ? STAGING_TYPES : RELEASE_TYPES;
        List<ModrinthVersion> newestFirst = versions.stream()
            .filter(version -> types.contains(version.type()) && majorVersion(version.version()) != null)
            .sorted(Comparator.comparing((ModrinthVersion version) -> new ComparableVersion(version.version())).reversed())
            .toList();
        return new Updates(currentVersion, platform, newestFirst);
    }

    /**
     * @return The newest version on the running major version that runs on this server, NULL if there is none.
     */
    public @Nullable ModrinthVersion latestOnMajor() {
        Integer major = majorVersion(currentVersion);
        return newestFirst.stream()
            .filter(version -> Objects.equals(majorVersion(version.version()), major) && platform.runs(version))
            .findFirst()
            .orElse(null);
    }

    /**
     * @return The regular update: the newest version on the running major version that runs on this server, if it
     * is newer than the running one; NULL otherwise.
     */
    public @Nullable ModrinthVersion update() {
        ModrinthVersion latest = latestOnMajor();
        return latest != null && isNewer(currentVersion, latest.version()) ? latest : null;
    }

    /**
     * @return The newest version with a higher major version than the running one, whatever it runs on; NULL if there
     * is none.
     */
    public @Nullable ModrinthVersion newMajor() {
        Integer major = majorVersion(currentVersion);
        if (major == null) {
            return null;
        }
        return newestFirst.stream()
            .filter(version -> Objects.requireNonNull(majorVersion(version.version())) > major)
            .findFirst()
            .orElse(null);
    }

    /**
     * @return The newest version that is newer than the running one, whatever it runs on; NULL if there is none.
     */
    public @Nullable ModrinthVersion newest() {
        return newestFirst.stream()
            .filter(version -> isNewer(currentVersion, version.version()))
            .findFirst()
            .orElse(null);
    }

    public static boolean isNewer(@NotNull String currentVersion, @NotNull String newVersion) {
        return new ComparableVersion(newVersion).compareTo(new ComparableVersion(currentVersion)) > 0;
    }

    /**
     * @return The major version, or NULL if the version doesn't start with major.minor.
     */
    static @Nullable Integer majorVersion(@NotNull String version) {
        Matcher matcher = VERSION.matcher(version);
        return matcher.matches() ? Integer.valueOf(matcher.group(1)) : null;
    }
}
