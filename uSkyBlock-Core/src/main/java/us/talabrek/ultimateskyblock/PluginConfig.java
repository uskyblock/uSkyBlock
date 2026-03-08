package us.talabrek.ultimateskyblock;

import com.google.inject.Inject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Logger;

public class PluginConfig {
    private static final Logger logger = Logger.getLogger(PluginConfig.class.getName());

    private final PluginConfigLoader loader = new PluginConfigLoader(logger);
    private YamlConfiguration yamlConfig;

    @Inject
    public PluginConfig() {
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
