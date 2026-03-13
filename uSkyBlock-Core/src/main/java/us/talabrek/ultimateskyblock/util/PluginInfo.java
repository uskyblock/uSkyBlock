package us.talabrek.ultimateskyblock.util;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.FormatUtil;
import dk.lockfuglsang.minecraft.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.plugin.UpdateChecker;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.handler.AsyncWorldEditHandler;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.player.PlayerLogic;

import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.miniToLegacy;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.uSkyBlock.depends;

public class PluginInfo {

    private final Plugin plugin;
    private final IslandLogic islandLogic;
    private final RuntimeConfigs runtimeConfigs;
    private final UpdateChecker updateChecker;
    private final PlayerLogic playerLogic;
    private final Logger logger;

    @Inject
    public PluginInfo(
        @NotNull Plugin plugin,
        @NotNull IslandLogic islandLogic,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull UpdateChecker updateChecker,
        @NotNull PlayerLogic playerLogic, Logger logger
    ) {
        this.plugin = plugin;
        this.islandLogic = islandLogic;
        this.runtimeConfigs = runtimeConfigs;
        this.updateChecker = updateChecker;
        this.playerLogic = playerLogic;
        this.logger = logger;
    }

    public void startup() {
        logger.info(FormatUtil.stripFormatting(getVersionInfo(false)));
    }

    public String getVersionInfo(boolean checkEnabled) {
        RuntimeConfig runtimeConfig = runtimeConfigs.current();
        PluginDescriptionFile description = plugin.getDescription();
        StringBuilder msg = new StringBuilder(miniToLegacy("<muted>Name: <primary><name><newline>",
            unparsed("name", description.getName())));
        msg.append(miniToLegacy("<muted>Version: <primary><version><newline>",
            unparsed("version", description.getVersion())));
        msg.append(miniToLegacy("<muted>Description: <primary><description><newline>",
            unparsed("description", description.getDescription())));
        msg.append(miniToLegacy("<muted>Language: <primary><language> (<locale>)<newline>",
            unparsed("language", runtimeConfig.configuredLanguage()),
            unparsed("locale", String.valueOf(I18nUtil.getI18n().getLocale()))));
        msg.append(miniToLegacy("<primary>  State: d=<distance>, r=<radius>, i=<islands>, p=<players>, n=<nether>, awe=<awe><newline>",
            number("distance", runtimeConfig.island().distance()),
            number("radius", runtimeConfig.island().radius()),
            number("islands", islandLogic.getSize()),
            number("players", playerLogic.getSize()),
            unparsed("nether", String.valueOf(runtimeConfig.nether().enabled())),
            unparsed("awe", String.valueOf(AsyncWorldEditHandler.isAWE()))));
        msg.append(miniToLegacy("<muted>Server: <primary><name> <version></primary><newline>",
            unparsed("name", Bukkit.getName()),
            unparsed("version", Bukkit.getVersion())));
        msg.append(miniToLegacy("<primary>  State: online=<online>, bungee=<bungee><newline>",
            unparsed("online", String.valueOf(ServerUtil.isOnlineMode())),
            unparsed("bungee", String.valueOf(ServerUtil.isBungeeEnabled()))));
        msg.append(miniToLegacy("<muted>------------------------------<newline>"));
        for (String[] dep : depends) {
            Plugin dependency = Bukkit.getPluginManager().getPlugin(dep[0]);
            if (dependency != null) {
                String status = miniToLegacy("N/A");
                if (checkEnabled) {
                    if (dependency.isEnabled()) {
                        if (VersionUtil.getVersion(dependency.getDescription().getVersion()).isLT(dep[1])) {
                            status = miniToLegacy("<error>WRONG-VERSION");
                        } else {
                            status = miniToLegacy("<secondary>ENABLED");
                        }
                    } else {
                        status = miniToLegacy("<error>DISABLED");
                    }
                }
                msg.append(miniToLegacy("<muted><primary><dependency> <version></primary><muted> (<status>)<newline>",
                    unparsed("dependency", dependency.getName()),
                    unparsed("version", dependency.getDescription().getVersion()),
                    legacyArg("status", status)));
            }
        }
        msg.append(miniToLegacy("<muted>------------------------------<newline>"));

        if (runtimeConfig.pluginUpdates().check() && updateChecker.isUpdateAvailable()) {
            msg.append(miniToLegacy("<muted>A new update of uSkyBlock is available: <version><newline>",
                unparsed("version", updateChecker.getLatestVersion(), PRIMARY)));
            msg.append(miniToLegacy("<muted>Visit <url> to download.<newline>",
                unparsed("url", "https://www.uskyblock.ovh/get", PRIMARY)));
        }

        return msg.toString();
    }
}
