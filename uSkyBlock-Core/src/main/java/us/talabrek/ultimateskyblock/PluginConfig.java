package us.talabrek.ultimateskyblock;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public class PluginConfig {

    @Inject
    public PluginConfig() {
        Settings.loadPluginConfig(getYamlConfig());
    }

    @NotNull
    public FileConfiguration getYamlConfig() {
        return FileUtil.getYmlConfiguration("config.yml");
    }
}
