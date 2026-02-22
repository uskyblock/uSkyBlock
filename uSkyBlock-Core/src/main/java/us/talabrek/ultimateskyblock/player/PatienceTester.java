package us.talabrek.ultimateskyblock.player;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.util.Msg.send;

public class PatienceTester {

    private static final Duration COOLDOWN = Duration.ofSeconds(30);

    public static boolean isRunning(Player player, String key) {
        if (player.hasMetadata(key)) {
            List<MetadataValue> metadata = player.getMetadata(key);
            MetadataValue metadataValue = !metadata.isEmpty() ? metadata.getFirst() : null;
            Instant cooldownEnd = metadataValue != null ? (Instant) metadataValue.value() : null;
            if (cooldownEnd == null || Instant.now().isAfter(cooldownEnd)) {
                player.removeMetadata(key, uSkyBlock.getInstance());
                return false;
            } else {
                send(player, getMessage());
                return true;
            }
        }
        return false;
    }

    private static Component getMessage() {
        int index = (new Random()).nextInt(5);
        return switch (index) {
            case 0 -> tr("<primary>Hold your horses! You have to be patient...");
            case 1 -> tr("<primary>Not really patient, are you?");
            case 2 -> tr("<primary>Be patient, young padawan");
            case 3 -> tr("<primary>Patience you MUST have, young padawan");
            case 4 -> tr("<primary>The two most powerful warriors are patience and time.");
            default -> throw new IllegalStateException("Unexpected value: " + index);
        };
    }

    public static void startRunning(Player player, String key) {
        player.setMetadata(key, new FixedMetadataValue(uSkyBlock.getInstance(), Instant.now().plus(COOLDOWN)));
    }

    public static void stopRunning(Player player, String key) {
        player.removeMetadata(key, uSkyBlock.getInstance());
    }
}
