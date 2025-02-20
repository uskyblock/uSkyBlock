package us.talabrek.ultimateskyblock.handler.placeholder;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.uSkyBlock;

public class PlaceholderModule {

    private final PlaceholderHandler placeholderHandler;
    private final PluginConfig config;
    private final ChatReplaceListener chatReplaceListener;
    private final ServerCommandReplaceListener serverCommandListener;
    private final TextPlaceholder textPlaceholder;
    private final Provider<MVdWPlaceholderAPI> mvdwPlaceholderProvider;

    @Inject
    public PlaceholderModule(
        @NotNull PlaceholderHandler placeholderHandler,
        @NotNull PluginConfig config,
        @NotNull ChatReplaceListener chatReplaceListener,
        @NotNull ServerCommandReplaceListener serverCommandListener,
        @NotNull TextPlaceholder textPlaceholder,
        @NotNull Provider<MVdWPlaceholderAPI> mvdwPlaceholderProvider
    ) {
        this.placeholderHandler = placeholderHandler;
        this.config = config;
        this.chatReplaceListener = chatReplaceListener;
        this.serverCommandListener = serverCommandListener;
        this.textPlaceholder = textPlaceholder;
        this.mvdwPlaceholderProvider = mvdwPlaceholderProvider;
    }

    public void startup(uSkyBlock plugin) {
        if (config.getYamlConfig().getBoolean("placeholder.chatplaceholder", false)) {
            plugin.getServer().getPluginManager().registerEvents(chatReplaceListener, plugin);
        }
        if (config.getYamlConfig().getBoolean("placeholder.servercommandplaceholder", false)) {
            plugin.getServer().getPluginManager().registerEvents(serverCommandListener, plugin);
        }

        if (config.getYamlConfig().getBoolean("placeholder.mvdwplaceholderapi", false)
            && Bukkit.getPluginManager().getPlugin("MVdWPlaceholderAPI") != null) {
            MVdWPlaceholderAPI mvdwPlaceholder = mvdwPlaceholderProvider.get();
            mvdwPlaceholder.setup(plugin);
            placeholderHandler.registerPlaceholders(mvdwPlaceholder);
        }
        placeholderHandler.registerPlaceholders(textPlaceholder);
    }
}
