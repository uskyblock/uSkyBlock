package us.talabrek.ultimateskyblock.platform;

import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.update.ServerPlatform;

/**
 * What uSkyBlock does differently on Paper and on Spigot, chosen once at the start ({@link #forServer}). Paper and its
 * forks, such as Purpur, get the implementations in Core's {@code paper} source set, compiled against paper-api; every
 * other server gets the Spigot ones.
 */
public interface Platform {
    /** @return Paper or Spigot */
    @NotNull String name();

    /** @return what the update check looks for: on Paper, Paper's plugins as well as Spigot's */
    @NotNull ServerPlatform serverPlatform(@NotNull Server server);

    @NotNull Teleports teleports();

    /** One line for the log at the start, so that a wrong detection shows: the platform, and what the server says. */
    default @NotNull String describe(@NotNull Server server) {
        return "Running on " + name() + " (" + server.getName() + " " + server.getVersion() + ")";
    }

    /**
     * The implementation for the running server. The Paper one is loaded by name, as the main source set compiles
     * against spigot-api and cannot see the {@code paper} source set.
     */
    static @NotNull Platform forServer() {
        if (!isPaper()) {
            return new SpigotPlatform();
        }
        try {
            return (Platform) Class.forName("us.talabrek.ultimateskyblock.platform.paper.PaperPlatform")
                .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("The Paper platform is missing from the plugin jar", e);
        }
    }

    /** @return whether the server has Paper's API: Paper and its forks */
    private static boolean isPaper() {
        try {
            // The API's own class loader: a Paper class some plugin shades does not make a Spigot server Paper.
            Class.forName("io.papermc.paper.ServerBuildInfo", false, Server.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
