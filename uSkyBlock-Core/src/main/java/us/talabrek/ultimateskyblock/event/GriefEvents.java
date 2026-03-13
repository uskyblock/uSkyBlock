package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Shulker;
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
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;

import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;

/**
 * Handling of mob-related events.
 */
@Singleton
public class GriefEvents implements Listener {

    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public GriefEvents(@NotNull uSkyBlock plugin, @NotNull RuntimeConfigs runtimeConfigs) {
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
    }

    private RuntimeConfig.Protection protection() {
        return runtimeConfigs.current().protection();
    }

    @EventHandler
    public void onCreeperExplode(ExplosionPrimeEvent event) {
        if (!protection().creepers() || !plugin.getWorldManager().isSkyAssociatedWorld(event.getEntity().getWorld())) {
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
        if (!protection().visitorShearingProtected() || !plugin.getWorldManager().isSkyAssociatedWorld(player.getWorld())) {
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
        RuntimeConfig.Protection protection = protection();
        if ((!protection.visitorKillAnimalsProtected() && !protection.visitorKillMonstersProtected())
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
            cancelMobDamage(event, protection);
        } else if (event.getDamager() instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) event.getDamager()).getShooter();
            if (!(shooter instanceof Player)) {
                return;
            }
            Player player = (Player) shooter;
            if (player.hasPermission("usb.mod.bypassprotection") || plugin.playerIsOnIsland(player)) {
                return;
            }
            cancelMobDamage(event, protection);
        }
    }

    private void cancelMobDamage(EntityDamageByEntityEvent event, RuntimeConfig.Protection protection) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }
        LimitLogic.CreatureType type = plugin.getLimitLogic().getCreatureType(livingEntity);
        if (protection.visitorKillAnimalsProtected() && type == LimitLogic.CreatureType.ANIMAL) {
            event.setCancelled(true);
        } else if (protection.visitorKillMonstersProtected() && type == LimitLogic.CreatureType.MONSTER) {
            event.setCancelled(true);
        } else if (protection.visitorKillMonstersProtected() && event.getEntity() instanceof Shulker) {
            // Shulker is a Golem, but should probably be protected if killMonsters is enabled
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTrampling(PlayerInteractEvent event) {
        if (!protection().visitorTramplingProtected() || !plugin.getWorldManager().isSkyAssociatedWorld(event.getPlayer().getWorld())) {
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
        if (!protection().withers() || !plugin.getWorldManager().isSkyAssociatedWorld(e.getEntity().getWorld())) {
            return;
        }
        if (e.getEntity() instanceof Wither && e.getTarget() != null) {
            handleWitherRampage(e, (Wither) e.getEntity(), e.getTarget().getLocation());
        }
    }

    @EventHandler
    public void onWitherSkullExplosion(EntityDamageByEntityEvent e) {
        if (!protection().withers()
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
                islandInfo.sendMessageToOnlineMembers(trLegacy("<error>Wither despawned!</error> It wandered too far from your island."));
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
        if (!protection().visitorHatchingProtected() || !plugin.getWorldManager().isSkyAssociatedWorld(e.getPlayer().getWorld())) {
            return;
        }
        if (!plugin.playerIsOnIsland(e.getPlayer())) {
            e.setHatching(false);
        }
    }
}
