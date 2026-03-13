package us.talabrek.ultimateskyblock.config.runtime;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.PluginConfig;

import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class RuntimeConfigs {
    private final PluginConfig pluginConfig;
    private final RuntimeConfigFactory runtimeConfigFactory;
    private final AtomicReference<RuntimeConfig> current = new AtomicReference<>();

    @Inject
    public RuntimeConfigs(@NotNull PluginConfig pluginConfig, @NotNull RuntimeConfigFactory runtimeConfigFactory) {
        this.pluginConfig = pluginConfig;
        this.runtimeConfigFactory = runtimeConfigFactory;
        reload();
    }

    @NotNull
    public RuntimeConfig current() {
        RuntimeConfig snapshot = current.get();
        return snapshot != null ? snapshot : reload();
    }

    @NotNull
    public RuntimeConfig reload() {
        RuntimeConfig snapshot = runtimeConfigFactory.load(pluginConfig.getYamlConfig());
        current.set(snapshot);
        return snapshot;
    }
}
