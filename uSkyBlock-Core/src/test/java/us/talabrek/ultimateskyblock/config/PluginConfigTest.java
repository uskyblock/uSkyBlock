package us.talabrek.ultimateskyblock.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PluginConfigTest {

    @Test
    public void getYamlConfigCachesTheLoadedConfiguration() {
        PluginConfigLoader loader = mock(PluginConfigLoader.class);
        YamlConfiguration config = new YamlConfiguration();
        when(loader.load()).thenReturn(config);

        PluginConfig pluginConfig = new PluginConfig(loader);

        assertSame(config, pluginConfig.getYamlConfig());
        assertSame(config, pluginConfig.getYamlConfig());
        verify(loader, times(1)).load();
    }

    @Test
    public void reloadReplacesTheCachedConfigurationFromDisk() {
        PluginConfigLoader loader = mock(PluginConfigLoader.class);
        YamlConfiguration first = new YamlConfiguration();
        YamlConfiguration second = new YamlConfiguration();
        when(loader.load()).thenReturn(first, second);

        PluginConfig pluginConfig = new PluginConfig(loader);

        assertSame(first, pluginConfig.getYamlConfig());
        assertSame(second, pluginConfig.reload());
        assertSame(second, pluginConfig.getYamlConfig());
        verify(loader, times(2)).load();
    }
}
