package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

/**
 * Handling of mob-related events.
 */
@Singleton
public class GriefEvents implements Listener {

    private final uSkyBlock plugin;
    private final boolean creeperEnabled;
    private final boolean shearingEnabled;
    private final boolean killMonstersEnabled;
    private final boolean killAnimalsEnabled;
    private final boolean tramplingEnabled;
    private final boolean witherEnabled;
    private final boolean hatchingEnabled;

    @Inject
    public GriefEvents(@NotNull uSkyBlock plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        creeperEnabled = config.getBoolean("options.protection.creepers", true);
        witherEnabled = config.getBoolean("options.protection.withers", true);
        shearingEnabled = config.getBoolean("options.protection.visitors.shearing", true);
        killMonstersEnabled = config.getBoolean("options.protection.visitors.kill-monsters", true);
        killAnimalsEnabled = config.getBoolean("options.protection.visitors.kill-animals", true);
        tramplingEnabled = config.getBoolean("options.protection.visitors.trampling", true);
        hatchingEnabled = config.getBoolean("options.protection.visitors.hatching", true);
    }

    @EventHandler
    public void onCreeperExplode(ExplosionPrimeEvent event) {
        if (!creeperEnabled || !plugin.getWorldManager().isSkyWorld(event.getEntity().getWorld())) {
            return;
        }
        if (event.getEntity() instanceof Creeper
            && !isValidTarget(((Creeper)event.getEntity()).getTarget()))
        {
            event.setCancelled(true);
        } else if (event.getEntity() instanceof TNTPrimed) {
            TNTPrimed tntPrimed = (TNTPrimed) event.getEntity();
            if (tntPrimed.getSource() instanceof Player && !isValidTarget(tntPrimed.getSource())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Valid targets are players belonging to the island.
     */
    private boolean isValidTarget(Entity target) {
        return target instanceof Player && plugin.playerIsOnIsland((Player)target);
    }

    @EventHandler
    public void onShearEvent(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        if (!shearingEnabled || !plugin.getWorldManager().isSkyAssociatedWorld(player.getWorld())) {
            return; // Not our concern
        }
        if (player.hasPermission("usb.mod.bypassprotection")) {
            return;
        }
        if (!plugin.playerIsOnIsland(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if ((!killAnimalsEnabled && !killMonstersEnabled)
                || !plugin.getWorldManager().isSkyAssociatedWorld(event.getDamager().getWorld())) {
            return;
        }
        if (!(event.getEntity() instanceof Creature)) {
            return;
        }
        if (event.getDamager() instanceof Player
                && !plugin.playerIsOnIsland((Player)event.getDamager())) {
            if (event.getDamager().hasPermission("usb.mod.bypassprotection")) {
                return;
            }
            cancelMobDamage(event);
        } else if (event.getDamager() instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) event.getDamager()).getShooter();
            if (!(shooter instanceof Player)) {
                return;
            }
            Player player = (Player) shooter;
            if (player.hasPermission("usb.mod.bypassprotection") || plugin.playerIsOnIsland(player)) {
                return;
            }
            cancelMobDamage(event);
        }
    }

    private void cancelMobDamage(EntityDamageByEntityEvent event) {
        if (killAnimalsEnabled && event.getEntity() instanceof Animals) {
            event.setCancelled(true);
        } else if (killMonstersEnabled && event.getEntity() instanceof Monster) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTrampling(PlayerInteractEvent event) {
        if (!tramplingEnabled || !plugin.getWorldManager().isSkyAssociatedWorld(event.getPlayer().getWorld())) {
            return;
        }
        if (event.getAction() == Action.PHYSICAL
                && !isValidTarget(event.getPlayer())
                && event.hasBlock()
                && event.getClickedBlock().getType() == Material.FARMLAND
                ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTargeting(EntityTargetLivingEntityEvent e) {
        if (!witherEnabled || !plugin.getWorldManager().isSkyAssociatedWorld(e.getEntity().getWorld())) {
            return;
        }
        if (e.getEntity() instanceof Wither && e.getTarget() != null) {
            handleWitherRampage(e, (Wither) e.getEntity(), e.getTarget().getLocation());
        }
    }

    @EventHandler
    public void onWitherSkullExplosion(EntityDamageByEntityEvent e) {
        if (!witherEnabled
                || !(e.getEntity() instanceof WitherSkull)
                || !plugin.getWorldManager().isSkyAssociatedWorld(e.getEntity().getWorld())) {
            return;
        }
        // Find owner
        ProjectileSource shooter = ((WitherSkull) e.getEntity()).getShooter();
        if (shooter instanceof Wither) {
            handleWitherRampage(e, (Wither) shooter, e.getDamager().getLocation());
        }
    }

    private void handleWitherRampage(Cancellable event, Wither shooter, Location targetLocation) {
        String withersIsland = getOwningIsland(shooter);
        String targetIsland = WorldGuardHandler.getIslandNameAt(targetLocation);
        if (targetIsland == null || !targetIsland.equals(withersIsland)) {
            event.setCancelled(true);
            checkWitherLeash(shooter, withersIsland);
        }
    }

    private void checkWitherLeash(@NotNull Wither shooter, @Nullable String withersIsland) {
        String currentIsland = WorldGuardHandler.getIslandNameAt(shooter.getLocation());
        if (currentIsland == null || !currentIsland.equals(withersIsland)) {
            shooter.remove();
            IslandInfo islandInfo = plugin.getIslandInfo(withersIsland);
            if (islandInfo != null) {
                islandInfo.sendMessageToOnlineMembers(I18nUtil.tr("\u00a7cWither Despawned!\u00a7e It wandered too far from your island."));
            }
        }
    }

    private @Nullable String getOwningIsland(@NotNull Wither wither) {
        PersistentDataContainer container = wither.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, WitherTagEvents.ENTITY_ORIGIN_METADATA);
        if (container.has(key, PersistentDataType.STRING)) {
            return container.get(key, PersistentDataType.STRING);
        } else {
            return null;
        }
    }

    @EventHandler
    public void onEgg(PlayerEggThrowEvent e) {
        if (!hatchingEnabled || !plugin.getWorldManager().isSkyAssociatedWorld(e.getPlayer().getWorld())) {
            return;
        }
        if (!plugin.playerIsOnIsland(e.getPlayer())) {
            e.setHatching(false);
        }
    }
}
