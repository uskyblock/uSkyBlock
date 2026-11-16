package us.talabrek.ultimateskyblock.platform.paper;

import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.platform.Platform;
import us.talabrek.ultimateskyblock.platform.Teleports;
import us.talabrek.ultimateskyblock.update.ServerPlatform;

/** Paper and its forks. */
public final class PaperPlatform implements Platform {
    @Override
    public @NotNull String name() {
        return "Paper";
    }

    @Override
    public @NotNull ServerPlatform serverPlatform(@NotNull Server server) {
        return ServerPlatform.paper(ServerPlatform.minecraftVersion(server.getBukkitVersion()));
    }

    @Override
    public @NotNull Teleports teleports() {
        return new PaperTeleports();
    }
}
