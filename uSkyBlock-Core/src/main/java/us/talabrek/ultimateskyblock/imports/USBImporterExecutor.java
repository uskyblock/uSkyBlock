package us.talabrek.ultimateskyblock.imports;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.util.Timer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.imports.fixuuidleader.UUIDLeaderImporter;
import us.talabrek.ultimateskyblock.imports.update.USBUpdateImporter;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.ProgressTracker;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.LogUtil.log;

/**
 * Delegates and batches the import.
 */
@Singleton
public class USBImporterExecutor {
    private final uSkyBlock plugin;
    private final ProgressTracker progressTracker;
    private final RuntimeConfigs runtimeConfigs;
    private List<USBImporter> importers;
    private volatile Timer timer;
    private volatile int countSuccess;
    private volatile int countSkip;
    private volatile int countFailed;

    @Inject
    public USBImporterExecutor(uSkyBlock plugin, RuntimeConfigs runtimeConfigs) {
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
        double progressEveryFraction = runtimeConfigs.current().importer().progressEveryFraction();
        Duration progressInterval = runtimeConfigs.current().importer().progressEvery();
        progressTracker = new ProgressTracker(Bukkit.getConsoleSender(),
            marktr("Progress: <progress_pct:'0%'> (<progress>/<total> - success:<success>, failed:<failed>, skipped:<skipped>) ~ <elapsed>"),
            progressEveryFraction,
            progressInterval);
    }

    public List<String> getImporterNames() {
        List<String> result = new ArrayList<>();
        for (USBImporter importer : getImporters()) {
            result.add(importer.getName());
        }
        return result;
    }

    private List<USBImporter> getImporters() {
        if (importers == null) {
            importers = new ArrayList<>();
            importers.add(new UUIDLeaderImporter());
            importers.add(new USBUpdateImporter(runtimeConfigs));
            ServiceLoader<USBImporter> serviceLoader = ServiceLoader.load(USBImporter.class, getClass().getClassLoader());
            for (USBImporter usbImporter : serviceLoader) {
                importers.add(usbImporter);
            }
        }
        return importers;
    }

    public USBImporter getImporter(String name) {
        for (USBImporter importer : getImporters()) {
            if (name.equalsIgnoreCase(importer.getName())) {
                return importer;
            }
        }
        return null;
    }

    public void importUSB(final CommandSender sender, String name) {
        if (name == null || sender == null) {
            throw new IllegalArgumentException("sender and name must be non-null");
        }
        final USBImporter importer = getImporter(name);
        if (importer == null) {
            sendErrorTr(sender, "No importer named <importer> found.",
                unparsed("importer", name, PRIMARY));
            return;
        }
        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> doImport(sender, importer));
    }

    private void doImport(CommandSender sender, USBImporter importer) {
        this.timer = Timer.start();
        importer.init(plugin);
        countSuccess = 0;
        countFailed = 0;
        countSkip = 0;
        final File[] files = importer.getFiles();
        log(Level.INFO, "Importing " + files.length + " files");
        if (files.length > 0) {
            doImport(sender, importer, files);
        } else {
            complete(sender, importer);
        }
    }

    private void doImport(final CommandSender sender, final USBImporter importer, final File[] files) {
        try {
            for (File file : files) {
                try {
                    Boolean status = importer.importFile(file);
                    if (status == null) {
                        countSkip++;
                        log(Level.FINE, "Successfully skipped file " + file);
                    } else if (status) {
                        countSuccess++;
                        log(Level.FINE, "Successfully imported file " + file);
                    } else {
                        countFailed++;
                        log(Level.WARNING, "Could not import file " + file);
                    }
                } catch (Throwable t) {
                    countFailed++;
                    log(Level.WARNING, "Could not import file " + file, t);
                }
                progressTracker.progressUpdate(
                    countSuccess + countFailed + countSkip,
                    files.length,
                    unparsed("success", String.valueOf(countSuccess)),
                    unparsed("failed", String.valueOf(countFailed)),
                    unparsed("skipped", String.valueOf(countSkip)),
                    unparsed("elapsed", timer.elapsedAsString())
                );
            }
        } finally {
            complete(sender, importer);
        }
    }

    private void complete(CommandSender sender, USBImporter importer) {
        importer.completed(countSuccess, countFailed, countSkip);
        sendTr(sender, "Converted <converted>/<total> files in <elapsed>.",
            unparsed("converted", String.valueOf(countSuccess), PRIMARY),
            unparsed("total", String.valueOf(countSuccess + countFailed), PRIMARY),
            unparsed("elapsed", timer.elapsedAsString(), PRIMARY));
        plugin.getConfig().set("importer." + importer.getName() + ".imported", true);
        plugin.saveConfig();
    }
}
