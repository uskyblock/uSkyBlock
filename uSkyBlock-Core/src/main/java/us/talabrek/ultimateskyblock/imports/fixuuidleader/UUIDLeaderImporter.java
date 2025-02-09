package us.talabrek.ultimateskyblock.imports.fixuuidleader;

import dk.lockfuglsang.minecraft.file.FileUtil;
import us.talabrek.ultimateskyblock.imports.USBImporter;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.UUIDUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fixes the ".err" islands.
 */
public class UUIDLeaderImporter implements USBImporter {
    private static final Logger log = Logger.getLogger(UUIDLeaderImporter.class.getName());
    private uSkyBlock plugin;

    public String getName() {
        return "fix-leader-uuid";
    }

    @Override
    public void init(uSkyBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public Boolean importFile(File file) {
        File dir = file.getParentFile();
        String ymlFile = FileUtil.getBasename(file.getName());
        String islandName = FileUtil.getBasename(ymlFile);
        try {
            // Backup
            Path ymlPath = new File(dir, ymlFile).toPath();
            Files.copy(ymlPath, new File(dir, islandName + ".bak").toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getIslandLogic().deleteIslandConfig(islandName);
            boolean changed = false;
            String leaderName = null;
            try (FileWriter fw = new FileWriter(ymlPath.toFile()); BufferedWriter out = new BufferedWriter(fw)) {
                for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                    if (line.contains("leader:")) {
                        leaderName = line.substring(line.indexOf("leader:") + 7).trim();
                    }
                    if (line.contains("leader-uuid:") && line.contains("!!java.util.UUID")) {
                        if (leaderName != null) {
                            int ix = line.indexOf("!!java");
                            out.write(line.substring(0, ix) + UUIDUtil.asString(plugin.getPlayerDB().getUUIDFromName(leaderName)) + "\n");
                        } // else - skip
                        changed = true;
                    } else {
                        out.write(line + "\n");
                    }
                }
            }
            return changed;
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to convert " + file, e);
        }
        return false;
    }

    @Override
    public File[] getFiles() {
        var result = plugin.getDataFolder().toPath()
            .resolve("islands").toFile()
            .listFiles((dir, name) -> name != null && name.endsWith(".err"));
        return Objects.requireNonNullElseGet(result, () -> new File[0]);
    }

    @Override
    public void completed(int success, int failed, int skipped) {
        plugin.getOrphanLogic().save();
    }
}
