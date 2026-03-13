package us.talabrek.ultimateskyblock.config.runtime;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.PluginConfig;

import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class RuntimeConfigs {
    private final PluginConfig pluginConfig;
    private final AtomicReference<RuntimeConfig> current = new AtomicReference<>();

    @Inject
    public RuntimeConfigs(@NotNull PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
        reload();
    }

    @NotNull
    public RuntimeConfig current() {
        RuntimeConfig snapshot = current.get();
        return snapshot != null ? snapshot : reload();
    }

    @NotNull
    public RuntimeConfig reload() {
        RuntimeConfig snapshot = RuntimeConfigFactory.load(pluginConfig.getYamlConfig());
        current.set(snapshot);
        return snapshot;
    }
}
