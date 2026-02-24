package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import dk.lockfuglsang.minecraft.util.FormatUtil;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.PluginInfo;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

/**
 * Debug control.
 */
public class DebugCommand extends CompositeCommand {

    private final PluginInfo pluginInfo;

    public static final Logger log = Logger.getLogger("us.talabrek.ultimateskyblock");
    private static Handler logHandler = null;

    @Inject
    public DebugCommand(@NotNull uSkyBlock plugin, @NotNull PluginInfo pluginInfo) {
        super("debug", "usb.admin.debug", marktr("control debugging"));
        this.pluginInfo = pluginInfo;

        add(new AbstractCommand("setlevel", null, "level", marktr("set debug-level")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                if (args.length == 1) {
                    setLogLevel(sender, args[0]);
                    return true;
                }
                return false;
            }
        });
        add(new AbstractCommand("enable|disable", null, marktr("toggle debug-logging")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                if (logHandler != null && alias.equals("disable")) {
                    disableLogging(sender);
                } else if (alias.equals("enable")) {
                    enableLogging(sender, plugin);
                } else {
                    sendErrorTr(sender, "Logging wasn't active, so you can't disable it!");
                }
                return true;
            }
        });
        add(new AbstractCommand("flush", null, marktr("flush current content of the logger to file.")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                if (logHandler != null) {
                    logHandler.flush();
                    sendTr(sender, "Log file has been flushed.");
                } else {
                    sendErrorTr(sender, "Logging is not enabled. <muted>Use <cmd>/usb debug enable</cmd>.");
                }
                return true;
            }
        });
        String debugLevel = plugin.getConfig().getString("options.advanced.debugLevel", null);
        if (debugLevel != null) {
            setLogLevel(plugin.getServer().getConsoleSender(), debugLevel);
        }
    }

    public void setLogLevel(CommandSender sender, String arg) {
        try {
            Level level = Level.parse(arg.toUpperCase());
            log.setLevel(level);
            uSkyBlock.getInstance().getLogger().setLevel(level);
            sendTr(sender, "Set debug level to <primary><loglevel></primary>.", unparsed("loglevel", level.getName()));
            enableLogging(sender, uSkyBlock.getInstance());
        } catch (Exception e) {
            sendErrorTr(sender, "Invalid argument, try INFO, FINE, FINER, FINEST");
        }
    }

    public static void disableLogging(CommandSender sender) {
        if (logHandler != null) {
            log.removeHandler(logHandler);
            uSkyBlock.getInstance().getLogger().removeHandler(logHandler);
            logHandler.close();
            if (sender != null) {
                sendTr(sender, "Logging disabled.");
            }
        }
        logHandler = null;
    }

    public void enableLogging(CommandSender sender, uSkyBlock plugin) {
        if (logHandler != null) {
            log.removeHandler(logHandler);
            plugin.getLogger().removeHandler(logHandler);
        }
        File logFolder = new File(plugin.getDataFolder(), "logs");
        logFolder.mkdirs();
        try {
            String logFile = logFolder + File.separator + "uskyblock.%u.log";
            logHandler = new FileHandler(logFile, true);
            logHandler.setFormatter(new SingleLineFormatter());
            log.addHandler(logHandler);
            plugin.getLogger().addHandler(logHandler);
            Level level = log.getLevel() != null ? log.getLevel() : Level.FINER;
            log.log(level, FormatUtil.stripFormatting(pluginInfo.getVersionInfo(true)));
            sendTr(sender, "Logging to <primary><logfile></primary>.", unparsed("logfile", logFile));
        } catch (IOException e) {
            log.log(Level.WARNING, "Unable to enable logging", e);
            sendErrorTr(sender, "Unable to enable logging: <reason>", unparsed("reason", e.getMessage()));
        }
    }

    public static class SingleLineFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String sourceClassName = record.getSourceClassName();
            sourceClassName = sourceClassName.substring(sourceClassName.lastIndexOf(".")+1);
            try {
                return String.format("%1$d %2$tY-%2$tm-%2$td %2$tH:%2$tM:%2$tS.%2$tL %3$s %4$s %5$s\n",
                        record.getMillis(), new Date(record.getMillis()), sourceClassName,
                        record.getSourceMethodName(),
                        MessageFormat.format(record.getMessage(), record.getParameters()));
            } catch (IllegalArgumentException e) {
                return String.format("%1$d %2$tY-%2$tm-%2$td %2$tH:%2$tM:%2$tS.%2$tL %3$s %4$s %5$s %6$s\n",
                        record.getMillis(), new Date(record.getMillis()), sourceClassName,
                        record.getSourceMethodName(),
                        record.getMessage(),
                        record.getParameters() != null ? Arrays.toString(record.getParameters()) : "");
            }
        }
    }
}
