package us.talabrek.ultimateskyblock.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.placeholder.PlaceholderSource;
import us.talabrek.ultimateskyblock.uSkyBlock;

/**
 * Bundled PlaceholderAPI expansion offering uSkyBlock values as
 * {@code %uskyblock_<key>%} (e.g. {@code %uskyblock_island_level%}).
 * Registered by {@link PapiIntegration} when PlaceholderAPI is installed.
 */
public class UskyblockExpansion extends PlaceholderExpansion {

    private final uSkyBlock plugin;
    private final PlaceholderSource source;

    public UskyblockExpansion(@NotNull uSkyBlock plugin, @NotNull PlaceholderSource source) {
        this.plugin = plugin;
        this.source = source;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "uskyblock";
    }

    @Override
    public @NotNull String getAuthor() {
        return "uSkyBlock";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @NotNull String getRequiredPlugin() {
        return "uSkyBlock";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        Component value;
        try {
            value = source.resolve(player, params);
        } catch (RuntimeException e) {
            return null; // corrupt/unloadable data: PAPI convention keeps the token visible
        }
        return value != null ? LegacyComponentSerializer.legacySection().serialize(value) : null;
    }
}
