package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.biome.BiomeConfig;
import us.talabrek.ultimateskyblock.biome.Biomes;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.task.SetBiomeTask;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;

public class BiomeCommand extends RequireIslandCommand {
    private final Biomes biomes;
    private final BiomeConfig biomeConfig;
    private final Scheduler scheduler;

    @Inject
    public BiomeCommand(@NotNull uSkyBlock plugin, @NotNull Biomes biomes, @NotNull BiomeConfig biomeConfig, @NotNull Scheduler scheduler) {
        super(plugin, "biome|b", null, "biome ?radius", marktr("change the biome of the island"));
        this.biomes = biomes;
        this.biomeConfig = biomeConfig;
        this.scheduler = scheduler;
        addFeaturePermission("usb.exempt.cooldown.biome", trLegacy("exempt player from biome-cooldown"));
        for (String biome : biomeConfig.getConfiguredBiomeKeys()) {
            addFeaturePermission("usb.biome." + biome,
                trLegacy("Let the player change their island's biome to <biome>", unparsed("biome", biome.toUpperCase())));
        }
    }

    @Override
    protected boolean doExecute(String alias, final Player player, PlayerInfo pi, final IslandInfo island, Map<String, Object> data, final String... args) {
        if (args.length == 0) {
            if (!island.hasPerm(player, "canChangeBiome")) {
                sendErrorTr(player, "You do not have permission to change the biome of your current island.");
            } else {
                biomes.openBiomeGui(player, island);
            }
        }
        if (args.length >= 1) {
            final String biomeKey = args[0].toLowerCase();
            if (!island.hasPerm(player, "canChangeBiome")) {
                sendErrorTr(player, "You do not have permission to change the biome of this island!");
                return true;
            }
            Location location = player.getLocation();
            ProtectedRegion region = WorldGuardHandler.getIslandRegionAt(location);
            if (!plugin.playerIsOnOwnIsland(player) || region == null) {
                sendErrorTr(player, "You must be on your island to change the biome.");
                return true;
            }
            Biome biome = Registry.BIOME.match(biomeKey);
            if (biome == null) {
                sendErrorTr(player, "You have misspelled the biome name. Must be one of <biomes>",
                    unparsed("biomes", String.join(", ", biomeConfig.getConfiguredBiomeKeys())));
                return true;
            }
            Duration cooldown = plugin.getCooldownHandler().getCooldown(player, "biome");
            if (cooldown.isPositive()) {
                sendTr(player, "You can change your biome again in <seconds> seconds.",
                    unparsed("seconds", String.valueOf(cooldown.toSeconds()), PRIMARY));
                return true;
            }
            if (!player.hasPermission("usb.biome." + biomeKey.toLowerCase())) {
                sendErrorTr(player, "You do not have permission to change your biome to that type.");
                return true;
            }
            BlockVector3 minP = region.getMinimumPoint();
            BlockVector3 maxP = region.getMaximumPoint();
            if (Settings.island_distance > Settings.island_protectionRange) {
                int buffer = (Settings.island_distance - Settings.island_protectionRange) / 2;
                minP.subtract(buffer, 0, buffer);
                maxP.add(buffer, 0, buffer);
            }
            boolean changeEntireIslandBiome;

            if (args.length == 2 && args[1].matches("[0-9]+")) {
                int radius = Integer.parseInt(args[1], 10);
                minP = BlockVector3.at(Math.max(location.getBlockX() - radius, minP.getBlockX()),
                    Math.max(location.getBlockY() - radius, minP.getBlockY()),
                    Math.max(location.getBlockZ() - radius, minP.getBlockZ()));
                maxP = BlockVector3.at(Math.min(location.getBlockX() + radius, maxP.getBlockX()),
                    Math.min(location.getBlockY() + radius, maxP.getBlockY()),
                    Math.min(location.getBlockZ() + radius, maxP.getBlockZ()));
                changeEntireIslandBiome = false;
                sendTr(player, "The pixies are busy changing the biome near you to <biome>, be patient.",
                    unparsed("biome", biomeKey, PRIMARY));
            } else if (args.length == 2 && args[1].equalsIgnoreCase("chunk")) {
                Chunk chunk = location.clone().getChunk();
                minP = BlockVector3.at(chunk.getX() << 4, location.getWorld().getMinHeight(), chunk.getZ() << 4);
                maxP = BlockVector3.at((chunk.getX() << 4) + 15, location.getWorld().getMaxHeight(), (chunk.getZ() << 4) + 15);
                changeEntireIslandBiome = false;
                sendTr(player, "The pixies are busy changing the biome in your current chunk to <biome>, be patient.",
                    unparsed("biome", biomeKey, PRIMARY));
            } else if (args.length < 2 || args[1].equalsIgnoreCase("all")) {
                changeEntireIslandBiome = true;
                sendTr(player, "The pixies are busy changing the biome of your island to <biome>, be patient.",
                    unparsed("biome", biomeKey, PRIMARY));
            } else {
                sendErrorTr(player, "Invalid arguments. Use /is biome [biome] [radius|chunk|all]");
                return true;
            }

            scheduler.sync(new SetBiomeTask(plugin, player.getWorld(), minP, maxP, biome, () -> {
                String biomeName = biome.getKey().getKey();
                if (changeEntireIslandBiome) {
                    island.setBiome(biome);
                    sendTr(player, "You have changed your island's biome to <biome>", SECONDARY, unparsed("biome", biomeName));
                    island.sendMessageToIslandGroup(tr("<player> changed the island biome to <biome>",
                        unparsed("player", player.getName()),
                        unparsed("biome", biomeName)));
                    plugin.getCooldownHandler().resetCooldown(player, "biome", Settings.general_biomeChange);
                } else {
                    sendTr(player, "You have changed <blocks> blocks around you to the <biome> biome",
                        SECONDARY,
                        unparsed("blocks", args[1]),
                        unparsed("biome", biomeName));
                    island.sendMessageToIslandGroup(tr("<player> created an area with a <biome> biome",
                        unparsed("player", player.getName()),
                        unparsed("biome", biomeName)));
                }
            }));
        }
        return true;
    }
}
