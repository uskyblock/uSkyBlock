package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendPlayerOnly;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

public class ChunkCommand extends CompositeCommand {

    @Inject
    public ChunkCommand(@NotNull uSkyBlock plugin) {
        super("chunk", "usb.admin.chunk", marktr("various chunk commands"));

        add(new RequireChunkCommand("regen", marktr("regenerate current chunk")) {
            @Override
            void doChunkCommand(Player player, Chunk chunk) {
                plugin.getWorldManager().getChunkRegenerator(chunk.getWorld()).regenerateChunk(chunk);
                sendTr(player, "Successfully regenerated chunk at <x>,<z>.",
                    unparsed("x", String.valueOf(chunk.getX())),
                    unparsed("z", String.valueOf(chunk.getZ())));
            }
        });
        add(new RequireChunkCommand("unload", marktr("unload current chunk")) {
            @Override
            void doChunkCommand(Player player, Chunk chunk) {
                if (chunk.getWorld().unloadChunk(chunk.getX(), chunk.getZ(), false)) {
                    sendTr(player, "Successfully unloaded chunk at <x>,<z>.",
                        unparsed("x", String.valueOf(chunk.getX())),
                        unparsed("z", String.valueOf(chunk.getZ())));
                } else {
                    sendErrorTr(player, "Failed to unload chunk at <x>,<z>.",
                        unparsed("x", String.valueOf(chunk.getX()), PRIMARY),
                        unparsed("z", String.valueOf(chunk.getZ()), PRIMARY));
                }
            }
        });
        add(new RequireChunkCommand("load", marktr("load current chunk")) {
            @Override
            void doChunkCommand(Player player, Chunk chunk) {
                chunk.getWorld().loadChunk(chunk.getX(), chunk.getZ(), true);
                sendTr(player, "Loaded chunk at <x>,<z>.",
                    unparsed("x", String.valueOf(chunk.getX())),
                    unparsed("z", String.valueOf(chunk.getZ())));
            }
        });
    }

    public abstract static class RequireChunkCommand extends AbstractCommand {
        public RequireChunkCommand(String name, String description) {
            super(name, null, "?x ?z ?r", description);
        }

        @Override
        public boolean execute(CommandSender commandSender, String alias, Map<String, Object> map, String... args) {
            if (!(commandSender instanceof Player)) {
                sendPlayerOnly(commandSender);
                return false;
            }
            Player player = (Player) commandSender;
            World world = player.getLocation().getWorld();
            int x = 0;
            int z = 0;
            int r = 0;
            if (args.length == 0) {
                Chunk chunk = player.getLocation().getChunk();
                x = chunk.getX();
                z = chunk.getZ();
            }
            if (args.length > 0 && args[0].matches("-?[0-9]+")) {
                x = Integer.parseInt(args[0], 10);
            }
            if (args.length > 1 && args[1].matches("-?[0-9]+")) {
                z = Integer.parseInt(args[1], 10);
            }
            if (args.length > 2 && args[2].matches("[0-9]+")) {
                r = Integer.parseInt(args[2], 10);
            }
            try {
                for (int cx = x - r; cx <= x + r; cx++) {
                    for (int cz = z - r; cz <= z + r; cz++) {
                        Chunk chunk = world.getChunkAt(cx, cz);
                        doChunkCommand(player, chunk);
                    }
                }
            } catch (Exception e) {
                sendErrorTr(player, "Error: <error>", unparsed("error", e.getMessage() != null ? e.getMessage() : ""));
            }
            return true;
        }

        abstract void doChunkCommand(Player player, Chunk chunk);
    }
}
