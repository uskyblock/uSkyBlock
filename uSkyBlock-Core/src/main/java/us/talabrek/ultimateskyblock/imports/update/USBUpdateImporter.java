package us.talabrek.ultimateskyblock.imports.update;

import dk.lockfuglsang.minecraft.file.FileUtil;
import us.talabrek.ultimateskyblock.imports.USBImporter;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.IslandUtil;

import java.io.File;
import java.util.Objects;

/**
 * An importer that simply loads all island-infos into memory - activating the built-in yml
 * file updating.
 */
public class USBUpdateImporter implements USBImporter {
    private uSkyBlock plugin;

    @Override
    public String getName() {
        return "update-islands";
    }

    @Override
    public void init(uSkyBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public Boolean importFile(File file) {
        String islandName = FileUtil.getBasename(file.getName());
        plugin.getIslandLogic().getIslandInfo(islandName);
        plugin.getIslandLogic().removeIslandFromMemory(islandName);
        return true;
    }

    @Override
    public File[] getFiles() {
        var result = plugin.getDataFolder().toPath()
            .resolve("islands").toFile()
            .listFiles(IslandUtil.createIslandFilenameFilter());
        return Objects.requireNonNullElseGet(result, () -> new File[0]);
    }

    @Override
    public void completed(int success, int failed, int skipped) {
        // Do nothing
    }
}
