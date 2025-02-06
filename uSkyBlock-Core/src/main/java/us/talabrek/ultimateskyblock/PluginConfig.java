package us.talabrek.ultimateskyblock;

import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public class PluginConfig {

    @NotNull
    public FileConfiguration getYamlConfig() {
        return FileUtil.getYmlConfiguration("config.yml");
    }
}
