package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.player.Perk;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static dk.lockfuglsang.minecraft.util.FormatUtil.stripFormatting;

public class PerkCommand extends CompositeCommand {

    @Inject
    public PerkCommand(@NotNull uSkyBlock plugin) {
        super("perk", "usb.admin.perk", marktr("shows perk-information"));
        add(new AbstractCommand("list", "lists all perks") {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Perk> entry : plugin.getPerkLogic().getPerkMap().entrySet()) {
                    sb.append("\u00a79").append(entry.getKey()).append(":\n");
                    String value = (entry.getValue().toString().replaceAll("\n", "\n  ")).trim();
                    sb.append("  ").append(value).append("\n");
                }
                sender.sendMessage(sb.toString().split("\n"));
                return true;
            }
        });
        add(new AbstractCommand("player", "", "player", marktr("shows a specific players perks")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                if (args.length == 1) {
                    Player player = plugin.getPlayerDB().getPlayer(args[0]);
                    if (player != null) {
                        StringBuilder sb = new StringBuilder();
                        Perk perk = plugin.getPerkLogic().getPerk(player);
                        sb.append("\u00a79").append(player.getName()).append(":\n");
                        String value = (perk.toString().replaceAll("\n", "\n  ")).trim();
                        sb.append("  ").append(value).append("\n");
                        sender.sendMessage(sb.toString().split("\n"));
                        return true;
                    } else {
                        sender.sendMessage(tr("\u00a74No player named {0} was found!", args[0]));
                    }
                }
                return false;
            }
        });
        for (Map.Entry<String, Perk> entry : plugin.getPerkLogic().getPerkMap().entrySet()) {
            addFeaturePermission(entry.getKey(), tr("additional perks {0}", stripFormatting(entry.getValue().toString().trim().replaceAll("\\n", ","))));
        }
    }
}
