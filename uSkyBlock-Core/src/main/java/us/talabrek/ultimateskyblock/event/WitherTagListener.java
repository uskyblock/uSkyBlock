package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

public class WitherTagListener implements Listener {

    static final String ENTITY_ORIGIN_METADATA = "from-island";
    private final uSkyBlock plugin;

    public WitherTagListener(uSkyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_WITHER
            && event.getEntity() instanceof Wither wither) {
            IslandInfo islandInfo = plugin.getIslandInfo(event.getLocation());
            if (islandInfo != null && islandInfo.getLeader() != null) {
                wither.setCustomName(I18nUtil.tr("{0}''s Wither", islandInfo.getLeader()));
                NamespacedKey key = new NamespacedKey(plugin, ENTITY_ORIGIN_METADATA);
                wither.getPersistentDataContainer().set(key, PersistentDataType.STRING, islandInfo.getName());
            }
        }
    }
}
