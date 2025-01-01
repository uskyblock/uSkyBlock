package us.talabrek.ultimateskyblock.imports.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import us.talabrek.ultimateskyblock.api.model.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDbImporter {
    private final uSkyBlock plugin;

    public PlayerDbImporter(uSkyBlock plugin) {
        this.plugin = plugin;
        importFile();
    }

    private void importFile() {
        File dataFile = new File(plugin.getDataFolder(), "uuid2name.yml");
        if (dataFile.exists()) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
                config.getKeys(false).forEach(key -> {
                    PlayerInfo playerInfo = new PlayerInfo(
                        UUID.fromString(key),
                        config.getString(key + ".name"),
                        config.getString(key + ".displayName"));

                    plugin.getStorage().savePlayerInfo(playerInfo);
                });

                Files.move(dataFile.toPath(), plugin.getDataFolder().toPath().resolve("uuid2name.old"));
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to move uuid2name.yml.", ex);
            }
        }
    }
}
