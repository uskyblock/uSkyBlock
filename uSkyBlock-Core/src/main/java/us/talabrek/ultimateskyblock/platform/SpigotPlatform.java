package us.talabrek.ultimateskyblock.platform;

import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.update.ServerPlatform;

/** Spigot, and every other server without Paper's API. */
final class SpigotPlatform implements Platform {
    @Override
    public @NotNull String name() {
        return "Spigot";
    }

    @Override
    public @NotNull ServerPlatform serverPlatform(@NotNull Server server) {
        return ServerPlatform.spigot(ServerPlatform.minecraftVersion(server.getBukkitVersion()));
    }

    @Override
    public @NotNull Teleports teleports() {
        return new SpigotTeleports();
    }
}
