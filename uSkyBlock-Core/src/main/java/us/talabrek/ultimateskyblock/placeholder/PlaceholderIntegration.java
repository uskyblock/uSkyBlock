package us.talabrek.ultimateskyblock.placeholder;

import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

/**
 * SPI for placeholder-broker integrations (e.g. PlaceholderAPI), discovered via
 * {@link java.util.ServiceLoader} and enabled only when the backing plugin is present.
 * An integration typically covers both directions: offering uSkyBlock values to the
 * broker, and contributing a TagResolver so broker placeholders render in our texts.
 */
public interface PlaceholderIntegration {

    /** Name of the backing Bukkit plugin, e.g. {@code PlaceholderAPI}. */
    @NotNull String pluginName();

    void enable(@NotNull PlaceholderSource source, @NotNull PlaceholderService service, @NotNull uSkyBlock plugin);
}
