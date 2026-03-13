package us.talabrek.ultimateskyblock.handler.placeholder;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.uSkyBlock;

public class PlaceholderModule {

    private final PlaceholderHandler placeholderHandler;
    private final RuntimeConfigs runtimeConfigs;
    private final ChatReplaceListener chatReplaceListener;
    private final ServerCommandReplaceListener serverCommandListener;
    private final TextPlaceholder textPlaceholder;
    private final Provider<MVdWPlaceholderAPI> mvdwPlaceholderProvider;

    @Inject
    public PlaceholderModule(
        @NotNull PlaceholderHandler placeholderHandler,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull ChatReplaceListener chatReplaceListener,
        @NotNull ServerCommandReplaceListener serverCommandListener,
        @NotNull TextPlaceholder textPlaceholder,
        @NotNull Provider<MVdWPlaceholderAPI> mvdwPlaceholderProvider
    ) {
        this.placeholderHandler = placeholderHandler;
        this.runtimeConfigs = runtimeConfigs;
        this.chatReplaceListener = chatReplaceListener;
        this.serverCommandListener = serverCommandListener;
        this.textPlaceholder = textPlaceholder;
        this.mvdwPlaceholderProvider = mvdwPlaceholderProvider;
    }

    public void startup(uSkyBlock plugin) {
        RuntimeConfig.Placeholder placeholder = runtimeConfigs.current().placeholder();
        if (placeholder.chatPlaceholder()) {
            plugin.getServer().getPluginManager().registerEvents(chatReplaceListener, plugin);
        }
        if (placeholder.serverCommandPlaceholder()) {
            plugin.getServer().getPluginManager().registerEvents(serverCommandListener, plugin);
        }

        if (placeholder.mvdwPlaceholderApi()
            && Bukkit.getPluginManager().getPlugin("MVdWPlaceholderAPI") != null) {
            MVdWPlaceholderAPI mvdwPlaceholder = mvdwPlaceholderProvider.get();
            mvdwPlaceholder.setup(plugin);
            placeholderHandler.registerPlaceholders(mvdwPlaceholder);
        }
        placeholderHandler.registerPlaceholders(textPlaceholder);
    }
}
