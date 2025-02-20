package us.talabrek.ultimateskyblock.handler.placeholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface PlaceholderAPI {

    interface PlaceholderReplacer {

        @NotNull Collection<String> getPlaceholders();

        @Nullable String replace(@Nullable OfflinePlayer offlinePlayer, @Nullable Player player, @Nullable String placeholder);
    }

    @Nullable String replacePlaceholders(@Nullable Player player, @Nullable String message);
}
