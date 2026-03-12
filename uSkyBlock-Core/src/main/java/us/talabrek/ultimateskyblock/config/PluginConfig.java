package us.talabrek.ultimateskyblock.config;

import com.google.inject.Inject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class PluginConfig {

    private final PluginConfigLoader loader;
    private YamlConfiguration yamlConfig;

    @Inject
    public PluginConfig(@NotNull PluginConfigLoader loader) {
        this.loader = loader;
    }

    @NotNull
    public synchronized YamlConfiguration getYamlConfig() {
        if (yamlConfig == null) {
            yamlConfig = loader.load();
        }
        return yamlConfig;
    }

    public synchronized void setYamlConfig(@NotNull FileConfiguration yamlConfig) {
        this.yamlConfig = (YamlConfiguration) yamlConfig;
    }

    public synchronized void save() throws IOException {
        loader.save(getYamlConfig());
    }
}
