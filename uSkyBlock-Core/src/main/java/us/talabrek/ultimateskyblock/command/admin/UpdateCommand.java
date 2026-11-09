package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.SkyUpdateChecker;
import us.talabrek.ultimateskyblock.handler.ConfirmHandler;
import us.talabrek.ultimateskyblock.update.ModrinthVersion;
import us.talabrek.ultimateskyblock.update.ServerPlatform;
import us.talabrek.ultimateskyblock.update.UpdateStager;
import us.talabrek.ultimateskyblock.update.Updates;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

/**
 * Stages the newest uSkyBlock version on the update channel for the next restart.
 */
public class UpdateCommand extends AbstractCommand {

    public static final String PERMISSION = "usb.admin.update";
    static final String COMMAND = "/usb update";

    private final SkyUpdateChecker updateChecker;
    private final UpdateStager stager;
    private final ConfirmHandler confirmHandler;
    private final Scheduler scheduler;

    @Inject
    public UpdateCommand(
        @NotNull SkyUpdateChecker updateChecker,
        @NotNull UpdateStager stager,
        @NotNull ConfirmHandler confirmHandler,
        @NotNull Scheduler scheduler
    ) {
        super("update", PERMISSION, marktr("stage the newest uSkyBlock version for the next restart"));
        this.updateChecker = updateChecker;
        this.stager = stager;
        this.confirmHandler = confirmHandler;
        this.scheduler = scheduler;
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        sendTr(sender, "Checking Modrinth for a newer uSkyBlock...", MUTED);
        updateChecker.fetchUpdates().thenAccept(updates -> scheduler.sync(() -> decide(sender, updates)));
        return true;
    }

    private void decide(@NotNull CommandSender sender, @Nullable Updates updates) {
        if (updates == null) {
            sendErrorTr(sender, "Could not reach Modrinth. Check for updates manually at <url>",
                link(ModrinthVersion.PROJECT_PAGE));
            return;
        }
        ModrinthVersion update = updates.update();
        if (update != null) {
            stage(sender, update);
            return;
        }
        ModrinthVersion newMajor = updates.newMajor();
        ModrinthVersion target = newMajor != null ? newMajor : updates.newest();
        if (target == null) {
            sendTr(sender, "uSkyBlock <version> is up to date.", unparsed("version", updates.currentVersion(), PRIMARY));
            return;
        }
        ServerPlatform platform = updates.platform();
        if (!platform.runs(target)) {
            sendErrorTr(sender, "uSkyBlock <version> needs Minecraft <minecraft> on <platforms>, but this server runs "
                    + "Minecraft <server-minecraft> on <server-platform>. Nothing was downloaded.",
                unparsed("version", target.version()),
                unparsed("minecraft", target.minecraftVersions()),
                unparsed("platforms", target.platforms()),
                unparsed("server-minecraft", platform.minecraftVersion()),
                unparsed("server-platform", platform.name()));
            return;
        }
        // Only a new major version can be newer and run here without being the regular update.
        sendTr(sender, "uSkyBlock <version> is a new major version. It changes your data and config at its first "
                + "start, and needs Minecraft <minecraft> on <platforms>.",
            unparsed("version", target.version(), PRIMARY),
            unparsed("minecraft", target.minecraftVersions()),
            unparsed("platforms", target.platforms()));
        sendTr(sender, "<error>Back up your server first</error>, and read the upgrade guide: <url>",
            link(SkyUpdateChecker.UPGRADE_GUIDE));
        sendTr(sender, "Release notes: <url>", link(target.page()));
        if (confirmHandler.checkCommand(sender, COMMAND)) {
            stage(sender, target);
        }
    }

    private void stage(@NotNull CommandSender sender, @NotNull ModrinthVersion version) {
        sendTr(sender, "Downloading uSkyBlock <version>...", MUTED, unparsed("version", version.version()));
        stager.stage(version, sender.getName(), false)
            .thenAccept(result -> scheduler.sync(() -> report(sender, result)));
    }

    private void report(@NotNull CommandSender sender, @NotNull UpdateStager.Result result) {
        ModrinthVersion version = result.version();
        UpdateStager.Failure failure = result.failure();
        if (failure == null) {
            if (result.replaced() != null) {
                sendTr(sender, "Replaced the staged uSkyBlock <previous> with <version>. It takes effect at the next restart.",
                    unparsed("previous", result.replaced()),
                    unparsed("version", version.version(), PRIMARY));
            } else {
                sendTr(sender, "uSkyBlock <version> is staged and takes effect at the next restart.",
                    unparsed("version", version.version(), PRIMARY));
            }
            return;
        }
        Path target = stager.target();
        switch (failure) {
            case NO_UPDATE_FOLDER -> sendErrorTr(sender, "Could not stage uSkyBlock <version>: the server has no "
                    + "update folder apart from the plugins folder. Set settings.update-folder in bukkit.yml.",
                unparsed("version", version.version()));
            case NOT_WRITABLE -> sendErrorTr(sender, "Could not stage uSkyBlock <version>: <folder> can't be written.",
                unparsed("version", version.version()),
                unparsed("folder", target != null ? String.valueOf(target.getParent()) : "?"));
            case NO_FILE -> sendErrorTr(sender, "Could not stage uSkyBlock <version>: Modrinth lists no file for it.",
                unparsed("version", version.version()));
            case HASH_MISMATCH -> sendErrorTr(sender, "Could not stage uSkyBlock <version>: the download doesn't match "
                    + "its SHA-512 hash on Modrinth, so it was deleted.",
                unparsed("version", version.version()));
            case UNREACHABLE -> sendErrorTr(sender, "Could not download uSkyBlock <version> from Modrinth. Try again "
                    + "later, or download it from <url>",
                unparsed("version", version.version()),
                link(version.page()));
            case BUSY -> sendErrorTr(sender, "Another update is being staged. Try again in a moment.");
        }
    }

    private static @NotNull TagResolver link(@NotNull URI url) {
        return component("url", Component.text(url.toString()).clickEvent(ClickEvent.openUrl(url.toString())), PRIMARY);
    }
}
