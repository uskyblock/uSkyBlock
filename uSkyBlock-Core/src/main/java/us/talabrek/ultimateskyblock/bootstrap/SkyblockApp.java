package us.talabrek.ultimateskyblock.bootstrap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.handler.placeholder.PlaceholderHandler;
import us.talabrek.ultimateskyblock.uSkyBlock;

@Singleton
public class SkyblockApp {

    private final Services services;
    private final Commands commands;
    private final Listeners listeners;

    @Inject
    public SkyblockApp(@NotNull Services services, @NotNull Commands commands, @NotNull Listeners listeners) {
        this.services = services;
        this.commands = commands;
        this.listeners = listeners;
    }


    public void startup(uSkyBlock plugin) {
        services.startup();
    }

    public void delayedEnable(uSkyBlock plugin) {
        services.delayedEnable(plugin);

        // do these really have to be delayed?
        commands.registerCommands(plugin);
        listeners.registerListeners(plugin);

        // TODO: make this object oriented
        PlaceholderHandler.register(plugin);
    }

    public void shutdown(uSkyBlock plugin) {
        PlaceholderHandler.unregister(plugin);
        Bukkit.getScheduler().cancelTasks(plugin);
        listeners.unregisterListeners(plugin);
        services.shutdown(plugin);
    }
}
