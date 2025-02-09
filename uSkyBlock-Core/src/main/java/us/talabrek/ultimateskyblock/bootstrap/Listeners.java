package us.talabrek.ultimateskyblock.bootstrap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.chat.ChatEvents;
import us.talabrek.ultimateskyblock.event.ExploitEvents;
import us.talabrek.ultimateskyblock.event.GriefEvents;
import us.talabrek.ultimateskyblock.event.InternalEvents;
import us.talabrek.ultimateskyblock.event.ItemDropEvents;
import us.talabrek.ultimateskyblock.event.MenuEvents;
import us.talabrek.ultimateskyblock.event.NetherTerraFormEvents;
import us.talabrek.ultimateskyblock.event.PlayerEvents;
import us.talabrek.ultimateskyblock.event.SpawnEvents;
import us.talabrek.ultimateskyblock.event.ToolMenuEvents;
import us.talabrek.ultimateskyblock.event.WitherTagEvents;
import us.talabrek.ultimateskyblock.event.WorldGuardEvents;
import us.talabrek.ultimateskyblock.gui.GuiListener;
import us.talabrek.ultimateskyblock.signs.SignEvents;
import us.talabrek.ultimateskyblock.command.InviteHandler;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

@Singleton
public class Listeners {

    private final PluginConfig config;

    private final GuiListener guiListener;
    private final InternalEvents internalEvents;
    private final PlayerEvents playerEvents;
    private final MenuEvents menuEvents;
    private final ExploitEvents exploitEvents;
    private final WitherTagEvents witherTagEvents;
    private final GriefEvents griefEvents;
    private final ItemDropEvents itemDropEvents;
    private final SpawnEvents spawnEvents;
    private final WorldGuardEvents worldGuardEvents;
    private final NetherTerraFormEvents netherTerraFormEvents;
    private final ToolMenuEvents toolMenuEvents;
    private final SignEvents signEvents;
    private final ChatEvents chatEvents;
    private final InviteHandler inviteHandler;
    private final PlayerDB playerDB;

    @Inject
    public Listeners(
        @NotNull PluginConfig config,
        @NotNull GuiListener guiListener,
        @NotNull InternalEvents internalEvents,
        @NotNull PlayerEvents playerEvents,
        @NotNull MenuEvents menuEvents,
        @NotNull ExploitEvents exploitEvents,
        @NotNull WitherTagEvents witherTagEvents,
        @NotNull GriefEvents griefEvents,
        @NotNull ItemDropEvents itemDropEvents,
        @NotNull SpawnEvents spawnEvents,
        @NotNull WorldGuardEvents worldGuardEvents,
        @NotNull NetherTerraFormEvents netherTerraFormEvents,
        @NotNull ToolMenuEvents toolMenuEvents,
        @NotNull SignEvents signEvents,
        @NotNull ChatEvents chatEvents,
        @NotNull InviteHandler inviteHandler,
        @NotNull PlayerDB playerDB
    ) {
        this.config = config;
        this.guiListener = guiListener;
        this.internalEvents = internalEvents;
        this.playerEvents = playerEvents;
        this.menuEvents = menuEvents;
        this.exploitEvents = exploitEvents;
        this.witherTagEvents = witherTagEvents;
        this.griefEvents = griefEvents;
        this.itemDropEvents = itemDropEvents;
        this.spawnEvents = spawnEvents;
        this.worldGuardEvents = worldGuardEvents;
        this.netherTerraFormEvents = netherTerraFormEvents;
        this.toolMenuEvents = toolMenuEvents;
        this.signEvents = signEvents;
        this.chatEvents = chatEvents;
        this.inviteHandler = inviteHandler;
        this.playerDB = playerDB;
    }

    public void registerListeners(Plugin plugin) {
        PluginManager manager = plugin.getServer().getPluginManager();

        manager.registerEvents(internalEvents, plugin);
        manager.registerEvents(playerEvents, plugin);
        manager.registerEvents(menuEvents, plugin);
        manager.registerEvents(guiListener, plugin);
        manager.registerEvents(exploitEvents, plugin);
        manager.registerEvents(witherTagEvents, plugin);
        manager.registerEvents(chatEvents, plugin);
        manager.registerEvents(inviteHandler, plugin);
        manager.registerEvents(playerDB, plugin);

        // TODO minoneer 06.02.2025: Move this logic. Either into the appropriate listener, or into submodules if we don't want all features active (e.g., the nether)
        if (config.getYamlConfig().getBoolean("options.protection.enabled", true)) {
            manager.registerEvents(griefEvents, plugin);
            if (config.getYamlConfig().getBoolean("options.protection.item-drops", true)) {
                manager.registerEvents(itemDropEvents, plugin);
            }
        }
        if (config.getYamlConfig().getBoolean("options.island.spawn-limits.enabled", true)) {
            manager.registerEvents(spawnEvents, plugin);
        }
        if (config.getYamlConfig().getBoolean("options.protection.visitors.block-banned-entry", true)) {
            manager.registerEvents(worldGuardEvents, plugin);
        }
        if (Settings.nether_enabled) {
            manager.registerEvents(netherTerraFormEvents, plugin);
        }
        if (config.getYamlConfig().getBoolean("tool-menu.enabled", true)) {
            manager.registerEvents(toolMenuEvents, plugin);
        }
        if (config.getYamlConfig().getBoolean("signs.enabled", true)) {
            manager.registerEvents(signEvents, plugin);
        }
    }

    public void unregisterListeners(Plugin plugin) {
        HandlerList.unregisterAll(plugin);
    }
}
