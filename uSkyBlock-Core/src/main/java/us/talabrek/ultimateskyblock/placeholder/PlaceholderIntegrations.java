package us.talabrek.ultimateskyblock.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Discovers {@link PlaceholderIntegration}s via ServiceLoader and enables those whose
 * backing plugin is installed and enabled. Implementations live in optional modules
 * (e.g. uSkyBlock-PAPI) and are merged into the shaded jar's service files.
 */
@Singleton
public class PlaceholderIntegrations {

    private final PlaceholderSource source;
    private final PlaceholderService service;
    private final Logger logger;

    @Inject
    public PlaceholderIntegrations(
        @NotNull PlaceholderSource source,
        @NotNull PlaceholderService service,
        @NotNull @PluginLog Logger logger
    ) {
        this.source = source;
        this.service = service;
        this.logger = logger;
    }

    public void startup(@NotNull uSkyBlock plugin) {
        for (PlaceholderIntegration integration : ServiceLoader.load(PlaceholderIntegration.class, getClass().getClassLoader())) {
            if (!plugin.getServer().getPluginManager().isPluginEnabled(integration.pluginName())) {
                continue;
            }
            try {
                integration.enable(source, service, plugin);
                logger.info("Enabled placeholder integration: " + integration.pluginName());
            } catch (RuntimeException | LinkageError e) {
                logger.log(Level.SEVERE, "Failed to enable placeholder integration " + integration.pluginName(), e);
            }
        }
    }
}
