package us.talabrek.ultimateskyblock.update;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.SkyUpdateChecker;
import us.talabrek.ultimateskyblock.command.admin.UpdateCommand;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Msg.CMD;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.send;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

/**
 * Tells players who may run {@code /usb update} about updates when they join.
 */
@Singleton
public class UpdateNotice implements Listener {

    // Late enough not to drown among the join messages.
    private static final Duration DELAY = Duration.ofSeconds(3);

    private final SkyUpdateChecker updateChecker;
    private final UpdateStager stager;
    private final RuntimeConfigs runtimeConfigs;
    private final Scheduler scheduler;

    @Inject
    public UpdateNotice(
        @NotNull SkyUpdateChecker updateChecker,
        @NotNull UpdateStager stager,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull Scheduler scheduler
    ) {
        this.updateChecker = updateChecker;
        this.stager = stager;
        this.runtimeConfigs = runtimeConfigs;
        this.scheduler = scheduler;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(UpdateCommand.PERMISSION)) {
            return;
        }
        scheduler.sync(() -> {
            if (player.isOnline()) {
                lines().forEach(line -> send(player, line));
            }
        }, DELAY);
    }

    /**
     * @return The notice, empty when uSkyBlock is up to date or update checks are off.
     */
    @NotNull List<Component> lines() {
        RuntimeConfig.PluginUpdates config = runtimeConfigs.current().pluginUpdates();
        if (!config.check()) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        Updates updates = updateChecker.getUpdates();
        UpdateStager.StagedUpdate staged = stager.staged();
        if (staged != null) {
            lines.add(staged.automatic()
                ? tr("uSkyBlock <version> was staged automatically and takes effect at the next restart.",
                    unparsed("version", staged.version(), PRIMARY))
                : tr("uSkyBlock <version> is staged and takes effect at the next restart.",
                    unparsed("version", staged.version(), PRIMARY)));
        }
        ModrinthVersion update = updates != null ? updates.update() : null;
        // With auto-stage, a regular update is auto-stage's to stage, and the staged line tells of it once it is.
        // Once auto-stage fails, admins have to stage it themselves.
        if (update != null && (!config.autoStage() || stager.autoStageFailed())
            && (staged == null || Updates.isNewer(staged.version(), update.version()))) {
            lines.add(tr("uSkyBlock <version> is available. <command> stages it for the next restart.",
                unparsed("version", update.version(), PRIMARY),
                component("command", Component.text("/usb update")
                    .clickEvent(ClickEvent.runCommand("/usb update")), CMD)));
        }
        ModrinthVersion newMajor = updates != null ? updates.newMajor() : null;
        if (newMajor != null && (staged == null || !staged.version().equals(newMajor.version()))) {
            String guide = SkyUpdateChecker.UPGRADE_GUIDE.toString();
            lines.add(tr("uSkyBlock <version> is out (needs Minecraft <minecraft>). Read the upgrade guide first: <url>",
                unparsed("version", newMajor.version(), PRIMARY),
                unparsed("minecraft", newMajor.minecraftVersions()),
                component("url", Component.text(guide).clickEvent(ClickEvent.openUrl(guide)), PRIMARY)));
        }
        return lines;
    }
}
