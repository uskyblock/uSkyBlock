package us.talabrek.ultimateskyblock.placeholder;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Source of uSkyBlock placeholder values, keyed by canonical, unprefixed names
 * (e.g. {@code island_level}). Implementations MUST be callable from any thread:
 * PlaceholderAPI expansions are polled from async scoreboard threads. Values that
 * can only be computed on the main thread must be served from snapshots and may
 * return a stand-in (e.g. {@code …}) until a main-thread refresh has run.
 */
public interface PlaceholderSource {

    @NotNull Set<String> keys();

    /**
     * Resolves the value of the given placeholder key for the given player.
     *
     * @return The value, or null if the key is unknown.
     */
    @Nullable Component resolve(@NotNull OfflinePlayer player, @NotNull String key);
}
