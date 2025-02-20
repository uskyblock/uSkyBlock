package us.talabrek.ultimateskyblock.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.animation.AnimationHandler;
import dk.lockfuglsang.minecraft.command.DocumentCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.SkyUpdateChecker;
import us.talabrek.ultimateskyblock.api.plugin.UpdateChecker;
import us.talabrek.ultimateskyblock.handler.placeholder.MVdWPlaceholderAPI;
import us.talabrek.ultimateskyblock.handler.placeholder.MvdwPlacehoderProvider;
import us.talabrek.ultimateskyblock.handler.placeholder.PlaceholderAPI;
import us.talabrek.ultimateskyblock.handler.placeholder.PlaceholderReplacerImpl;
import us.talabrek.ultimateskyblock.island.level.ChunkSnapshotLevelLogic;
import us.talabrek.ultimateskyblock.island.level.LevelLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.uuid.BukkitPlayerDB;
import us.talabrek.ultimateskyblock.uuid.FilePlayerDB;
import us.talabrek.ultimateskyblock.uuid.MemoryPlayerDB;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.nio.file.Path;
import java.time.Clock;
import java.util.logging.Logger;

public class SkyblockModule extends AbstractModule {

    private final uSkyBlock plugin;

    public SkyblockModule(uSkyBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // TODO: this should not be injected, but it is here fore legacy reasons. Move all functionality out of the plugin class and into the appropriate classes.
        bind(uSkyBlock.class).toInstance(plugin);
        bind(Plugin.class).toInstance(plugin);
        bind(Path.class).annotatedWith(PluginDataDir.class).toInstance(plugin.getDataFolder().toPath());
        bind(LevelLogic.class).to(ChunkSnapshotLevelLogic.class);
        bind(UpdateChecker.class).to(SkyUpdateChecker.class);
        bind(Clock.class).toInstance(Clock.systemUTC());
        bind(PlaceholderAPI.PlaceholderReplacer.class).to(PlaceholderReplacerImpl.class);
        bind(MVdWPlaceholderAPI.class).toProvider(MvdwPlacehoderProvider.class);
    }

    @Provides
    @Singleton
    public static
    @NotNull PlayerDB providePlayerDB(PluginConfig config, uSkyBlock plugin, Scheduler scheduler, Logger logger) {
        String playerDbStorage = config.getYamlConfig().getString("options.advanced.playerdb.storage", "yml");
        if (playerDbStorage.equalsIgnoreCase("yml")) {
            return new FilePlayerDB(plugin, scheduler, logger);
        } else if (playerDbStorage.equalsIgnoreCase("memory")) {
            return new MemoryPlayerDB(config);
        } else {
            return new BukkitPlayerDB();
        }
    }

    @Provides
    @Singleton
    public static @NotNull AnimationHandler provideAnimationHandler(Plugin plugin) {
        return new AnimationHandler(plugin);
    }

    @Provides
    public static @NotNull DocumentCommand provideDocumentCommand(uSkyBlock plugin) {
        return new DocumentCommand(plugin, "doc", "usb.admin.doc");
    }
}
