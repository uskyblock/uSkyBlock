package us.talabrek.ultimateskyblock.hook;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.util.VersionUtil;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.hook.economy.EconomyHook;
import us.talabrek.ultimateskyblock.hook.economy.VaultEconomy;
import us.talabrek.ultimateskyblock.hook.permissions.PermissionsHook;
import us.talabrek.ultimateskyblock.hook.permissions.VaultPermissions;
import us.talabrek.ultimateskyblock.hook.world.InventorySyncHook;
import us.talabrek.ultimateskyblock.hook.world.MultiverseCoreHook;
import us.talabrek.ultimateskyblock.hook.world.MultiverseInventoriesHook;
import us.talabrek.ultimateskyblock.hook.world.WorldHook;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class HookManager {
    private final uSkyBlock plugin;
    private final Logger logger;
    private final Map<String, PluginHook> hooks = new ConcurrentHashMap<>();

    @Inject
    public HookManager(@NotNull uSkyBlock plugin, @NotNull Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    /**
     * Returns an {@link Optional} containing the requested {@link PluginHook}, or null if the hook is not available.
     *
     * @param hook Name of the requested hook.
     * @return Optional containing the requested PluginHook, or null if unavailable.
     */
    public Optional<PluginHook> getHook(String hook) {
        return Optional.ofNullable(hooks.get(hook));
    }

    /**
     * Short method for {@link #getHook(String)} to get the optional {@link EconomyHook}.
     *
     * @return optional of EconomyHook.
     */
    public Optional<EconomyHook> getEconomyHook() {
        return Optional.ofNullable((EconomyHook) getHook("Economy").orElse(null));
    }

    /**
     * Short method for {@link #getHook(String)} to get the optional {@link WorldHook}.
     *
     * @return optional of WorldHook.
     */
    public Optional<WorldHook> getWorldHook() {
        return Optional.ofNullable((WorldHook) getHook("World").orElse(null));
    }

    /**
     * Short method for {@link #getHook(String)} to get the optional {@link InventorySyncHook}.
     *
     * @return optional of InventorySyncHook.
     */
    public Optional<InventorySyncHook> getInventorySyncHook() {
        return Optional.ofNullable((InventorySyncHook) getHook("InventorySync").orElse(null));
    }

    /**
     * Short method for {@link #getHook(String)} to get the optional {@link PermissionsHook}.
     *
     * @return optional of PermissionsHook.
     */
    public Optional<PermissionsHook> getPermissionsHook() {
        return Optional.ofNullable((PermissionsHook) getHook("Permissions").orElse(null));
    }

    /**
     * Tries to enable the hook in the given {@link PluginHook}. Adds the plugin hook to the list of enabled hooks
     * if successfull. Throws a {@link HookFailedException} otherwise.
     *
     * @param hook Hook to enable and register.
     * @throws HookFailedException if hooking into the plugin failes.
     */
    public void registerHook(PluginHook hook) throws HookFailedException {
        hook.onHook();
        hooks.put(hook.getHookName(), hook);
    }

    public void setupHooks() {
        setupMultiverse();
        setupEconomyHook();
        setupPermissionsHook();
    }

    /**
     * Checks and hooks if there are compatible Economy plugins available.
     *
     * @return True if a compatible Economy plugin has been found and hooking succeeded, false otherwise.
     */
    public boolean setupEconomyHook() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                VaultEconomy vault = new VaultEconomy(plugin);
                registerHook(vault);
                logger.info("Hooked into Vault economy");
                return true;
            }
        } catch (HookFailedException ex) {
            logger.log(Level.SEVERE, "Failed to hook into Vault economy.", ex);
        }

        logger.info("Failed to find a compatible economy system. Economy rewards will be disabled.");
        return false;
    }

    private static @Nullable String getPluginVersion(String pluginName) {
        var plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin == null ? null : plugin.getDescription().getVersion();
    }

    /**
     * Checks and hooks into Multiverse-Core and Multiverse-Inventories.
     */
    public void setupMultiverse() {
        try {
            var mvVersion = getPluginVersion("Multiverse-Core");
            if (mvVersion == null) {
                logger.info("Did not find Multiverse-Core. Skipping multi world setup.");
                return;
            }
            if (VersionUtil.getVersion(mvVersion).isLT("5.0.0")) {
                logger.info("Requires Multiverse-Core version 5 - found version " + mvVersion + ". Skipping multi world setup.");
                return;
            }
            WorldHook mvHook = new MultiverseCoreHook(plugin);
            registerHook(mvHook);
            logger.info("Hooked into Multiverse-Core");
        } catch (HookFailedException ex) {
            logger.log(Level.SEVERE, "Failed to hook into Multiverse-Core", ex);
        }

        try {
            var mvInvVersion = getPluginVersion("Multiverse-Inventories");
            if (mvInvVersion == null) {
                logger.info("Did not find Multiverse-Inventories. Inventory isolation will not be configured automatically.");
                return;
            }
            if (VersionUtil.getVersion(mvInvVersion).isLT("5.0.0")) {
                logger.info("Requires Multiverse-Inventories version 5 - found version " + mvInvVersion + ". Inventory isolation will not be configured automatically.");
                return;
            }
            InventorySyncHook mvInvHook = new MultiverseInventoriesHook(plugin);
            registerHook(mvInvHook);
            logger.info("Hooked into Multiverse-Inventories");
        } catch (HookFailedException ex) {
            logger.log(Level.SEVERE, "Failed to hook into Multiverse-Inventories", ex);
        }
    }

    /**
     * Checks and hooks if there are compatible Permissions plugins available.
     *
     * @return True if a compatible Permissions plugin has geen found and hooking succeeded, false otherwise.
     */
    public boolean setupPermissionsHook() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                VaultPermissions vault = new VaultPermissions(plugin);
                registerHook(vault);
                logger.info("Hooked into Vault permissions.");
                return true;
            }
        } catch (HookFailedException ex) {
            logger.log(Level.SEVERE, "Failed to hook into Vault permissions plugin.", ex);
        }

        logger.warning("Failed to find a compatible permissions system. Permission rewards will be disabled.");
        return false;
    }
}
