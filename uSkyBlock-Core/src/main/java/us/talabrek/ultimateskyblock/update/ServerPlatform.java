package us.talabrek.ultimateskyblock.update;

import io.papermc.lib.PaperLib;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the server runs, to match against a Modrinth version's {@code game_versions} and {@code loaders}.
 *
 * @param minecraftVersion The Minecraft version, e.g. 26.3.
 * @param name             The platform, e.g. Paper.
 * @param loaders          The Modrinth loaders whose plugins the platform runs.
 */
public record ServerPlatform(@NotNull String minecraftVersion, @NotNull String name, @NotNull Set<String> loaders) {

    // The leading dotted numbers of the Bukkit version: 26.3-R0.1-SNAPSHOT on Spigot, 26.3.build.41-alpha on Paper.
    private static final Pattern MINECRAFT_VERSION = Pattern.compile("\\d+(?:\\.\\d+)*");

    public static @NotNull ServerPlatform of(@NotNull Server server) {
        String minecraftVersion = minecraftVersion(server.getBukkitVersion());
        return PaperLib.isPaper() ? paper(minecraftVersion) : spigot(minecraftVersion);
    }

    static @NotNull String minecraftVersion(@NotNull String bukkitVersion) {
        Matcher matcher = MINECRAFT_VERSION.matcher(bukkitVersion);
        return matcher.lookingAt() ? matcher.group() : bukkitVersion;
    }

    public static @NotNull ServerPlatform paper(@NotNull String minecraftVersion) {
        return new ServerPlatform(minecraftVersion, "Paper", Set.of("paper", "spigot", "bukkit"));
    }

    public static @NotNull ServerPlatform spigot(@NotNull String minecraftVersion) {
        return new ServerPlatform(minecraftVersion, "Spigot", Set.of("spigot", "bukkit"));
    }

    public boolean runs(@NotNull ModrinthVersion version) {
        return version.gameVersions().contains(minecraftVersion) && version.loaders().stream().anyMatch(loaders::contains);
    }
}
