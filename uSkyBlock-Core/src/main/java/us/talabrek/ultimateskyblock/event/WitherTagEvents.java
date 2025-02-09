package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

@Singleton
public class WitherTagEvents implements Listener {

    static final String ENTITY_ORIGIN_METADATA = "from-island";
    private final uSkyBlock plugin;

    @Inject
    public WitherTagEvents(@NotNull uSkyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
