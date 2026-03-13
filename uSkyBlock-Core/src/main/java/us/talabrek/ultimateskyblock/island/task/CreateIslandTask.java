package us.talabrek.ultimateskyblock.island.task;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.player.PlayerPerk;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;

/**
 * Task instead of anonymous runnable - so we get some info on /timings paste
 */
public class CreateIslandTask extends BukkitRunnable {
    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;
    private final Player player;
    private final PlayerPerk playerPerk;
    private final Location next;
    private final String cSchem;
    private final Scheduler scheduler;

    public CreateIslandTask(uSkyBlock plugin, RuntimeConfigs runtimeConfigs, Player player, PlayerPerk playerPerk, Location next, String cSchem) {
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
        this.scheduler = plugin.getScheduler();
        this.player = player;
        this.playerPerk = playerPerk;
        this.next = next;
        this.cSchem = cSchem;
    }

    @Override
    public void run() {
        if (!plugin.getIslandGenerator().createIsland(next, cSchem)) {
            sendTr(player, "Unable to locate schematic <schematic>, contact a server admin",
                unparsed("schematic", cSchem));
        }
        GenerateTask generateTask = new GenerateTask(plugin, runtimeConfigs, player, playerPerk.getPlayerInfo(), next, playerPerk, cSchem);
        Duration heartBeat = runtimeConfigs.current().asyncWorldEdit().heartBeat();
        final BukkitRunnable completionWatchDog = new LocateChestTask(plugin, runtimeConfigs, player, next, generateTask);
        scheduler.sync(completionWatchDog, Duration.ZERO, heartBeat);
    }
}
