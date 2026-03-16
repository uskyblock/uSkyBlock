package us.talabrek.ultimateskyblock.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.animation.AnimationHandler;
import dk.lockfuglsang.minecraft.command.DocumentCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.SkyUpdateChecker;
import us.talabrek.ultimateskyblock.api.plugin.UpdateChecker;
import us.talabrek.ultimateskyblock.config.PluginConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.challenge.ChallengeProgressRepository;
import us.talabrek.ultimateskyblock.challenge.SqliteChallengeProgressRepository;
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
    private final PluginConfig pluginConfig;

    public SkyblockModule(uSkyBlock plugin, PluginConfig pluginConfig) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
    }

    @Override
    protected void configure() {
        // TODO: this should not be injected, but it is here fore legacy reasons. Move all functionality out of the plugin class and into the appropriate classes.
        bind(uSkyBlock.class).toInstance(plugin);
        bind(Plugin.class).toInstance(plugin);
        bind(Logger.class).annotatedWith(PluginLog.class).toInstance(plugin.getLogger());
        bind(PluginConfig.class).toInstance(pluginConfig);
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
    @NotNull PlayerDB providePlayerDB(RuntimeConfigs runtimeConfigs, uSkyBlock plugin, Scheduler scheduler, @PluginLog Logger logger) {
        String playerDbStorage = runtimeConfigs.current().advanced().playerDb().storage();
        if (playerDbStorage.equalsIgnoreCase("yml")) {
            return new FilePlayerDB(plugin, scheduler, logger, runtimeConfigs.current().advanced().playerDb());
        } else if (playerDbStorage.equalsIgnoreCase("memory")) {
            return new MemoryPlayerDB(runtimeConfigs.current().advanced().playerDb());
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
    @Singleton
    public static @NotNull ChallengeProgressRepository provideChallengeProgressRepository(
        @PluginDataDir Path pluginDataDir,
        Logger logger
    ) {
        return new SqliteChallengeProgressRepository(pluginDataDir.resolve("challenge-progress.db"), logger);
    }

    @Provides
    public static @NotNull DocumentCommand provideDocumentCommand(uSkyBlock plugin) {
        return new DocumentCommand(plugin, "doc", "usb.admin.doc");
    }
}
