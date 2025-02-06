package us.talabrek.ultimateskyblock.util;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.FormatUtil;
import dk.lockfuglsang.minecraft.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.plugin.UpdateChecker;
import us.talabrek.ultimateskyblock.handler.AsyncWorldEditHandler;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.player.PlayerLogic;

import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.pre;
import static us.talabrek.ultimateskyblock.uSkyBlock.depends;

public class PluginInfo {

    private final Plugin plugin;
    private final IslandLogic islandLogic;
    private final PluginConfig config;
    private final UpdateChecker updateChecker;
    private final PlayerLogic playerLogic;
    private final Logger logger;

    @Inject
    public PluginInfo(
        @NotNull Plugin plugin,
        @NotNull IslandLogic islandLogic,
        @NotNull PluginConfig config,
        @NotNull UpdateChecker updateChecker,
        @NotNull PlayerLogic playerLogic, Logger logger
    ) {
        this.plugin = plugin;
        this.islandLogic = islandLogic;
        this.config = config;
        this.updateChecker = updateChecker;
        this.playerLogic = playerLogic;
        this.logger = logger;
    }

    public void startup() {
        logger.info(FormatUtil.stripFormatting(getVersionInfo(false)));
    }

    public String getVersionInfo(boolean checkEnabled) {
        PluginDescriptionFile description = plugin.getDescription();
        StringBuilder msg = new StringBuilder(pre("\u00a77Name: \u00a7b{0}\n", description.getName()));
        msg.append(pre("\u00a77Version: \u00a7b{0}\n", description.getVersion()));
        msg.append(pre("\u00a77Description: \u00a7b{0}\n", description.getDescription()));
        msg.append(pre("\u00a77Language: \u00a7b{0} ({1})\n", config.getYamlConfig().get("language", "en"), I18nUtil.getI18n().getLocale()));
        msg.append(pre("\u00a79  State: d={0}, r={1}, i={2}, p={3}, n={4}, awe={5}\n", Settings.island_distance, Settings.island_radius,
            islandLogic.getSize(), playerLogic.getSize(),
            Settings.nether_enabled, AsyncWorldEditHandler.isAWE()));
        msg.append(pre("\u00a77Server: \u00a7e{0} {1}\n", Bukkit.getName(), Bukkit.getVersion()));
        msg.append(pre("\u00a79  State: online={0}, bungee={1}\n", ServerUtil.isOnlineMode(),
            ServerUtil.isBungeeEnabled()));
        msg.append(pre("\u00a77------------------------------\n"));
        for (String[] dep : depends) {
            Plugin dependency = Bukkit.getPluginManager().getPlugin(dep[0]);
            if (dependency != null) {
                String status = pre("N/A");
                if (checkEnabled) {
                    if (dependency.isEnabled()) {
                        if (VersionUtil.getVersion(dependency.getDescription().getVersion()).isLT(dep[1])) {
                            status = pre("\u00a7eWRONG-VERSION");
                        } else {
                            status = pre("\u00a72ENABLED");
                        }
                    } else {
                        status = pre("\u00a74DISABLED");
                    }
                }
                msg.append(pre("\u00a77\u00a7d{0} \u00a7f{1} \u00a77({2}\u00a77)\n", dependency.getName(),
                    dependency.getDescription().getVersion(), status));
            }
        }
        msg.append(pre("\u00a77------------------------------\n"));

        if (config.getYamlConfig().getBoolean("plugin-updates.check", true) && updateChecker.isUpdateAvailable()) {
            msg.append(pre("\u00a7bA new update of uSkyBlock is available: \u00a7f{0}\n", updateChecker.getLatestVersion()));
            msg.append(pre("\u00a7fVisit {0} to download.\n", "https://www.uskyblock.ovh/get"));
        }

        return msg.toString();
    }
}
