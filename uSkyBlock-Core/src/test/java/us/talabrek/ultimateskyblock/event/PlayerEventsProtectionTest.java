package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.logging.Logger;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for the mockable protection branches of {@link PlayerEvents}.
 *
 * <p>The banned / locked teleport branch ({@code onTeleport}) is intentionally not covered here: it resolves the
 * target island through {@code WorldGuardHandler.getIslandNameAt}, which is a static call into WorldGuard (compileOnly,
 * not on the test classpath). That path is left to the live-server harness.
 */
public class PlayerEventsProtectionTest {
    private uSkyBlock plugin;
    private WorldManager worldManager;
    private RuntimeConfigs runtimeConfigs;
    private PlayerEvents listener;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        plugin = mock(uSkyBlock.class);
        worldManager = mock(WorldManager.class);
        when(plugin.getWorldManager()).thenReturn(worldManager);
        when(worldManager.isSkyAssociatedWorld(any())).thenReturn(true);
        runtimeConfigs = mock(RuntimeConfigs.class);
        listener = new PlayerEvents(plugin, runtimeConfigs);
    }

    @Test
    public void onVisitorDamage_cancelsFireDamageForVisitor() {
        useConfig(true, true, false, false, false, "deny");
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(mock(World.class));
        when(plugin.playerIsOnIsland(player)).thenReturn(false);

        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.LAVA);
        when(event.getDamage()).thenReturn(6.0);

        listener.onVisitorDamage(event);

        verify(event).setDamage(-6.0);
        verify(event).setCancelled(true);
    }

    @Test
    public void onVisitorDamage_cancelsFallDamageForVisitor() {
        useConfig(true, false, true, false, false, "deny");
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(mock(World.class));
        when(plugin.playerIsOnIsland(player)).thenReturn(false);

        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.FALL);
        when(event.getDamage()).thenReturn(3.0);

        listener.onVisitorDamage(event);

        verify(event).setDamage(-3.0);
        verify(event).setCancelled(true);
    }

    @Test
    public void onVisitorDamage_ignoresPlayerOnOwnIsland() {
        useConfig(true, true, true, false, false, "deny");
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(mock(World.class));
        when(plugin.playerIsOnIsland(player)).thenReturn(true);

        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);

        listener.onVisitorDamage(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    public void onVisitorDamageByEntity_cancelsMonsterDamageForVisitor() {
        useConfig(true, false, false, true, false, "deny");
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(mock(World.class));
        when(plugin.playerIsOnIsland(player)).thenReturn(false);
        Entity damager = mock(Entity.class);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getDamager()).thenReturn(damager);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        when(event.getDamage()).thenReturn(4.0);

        listener.onVisitorDamageByEntity(event);

        verify(event).setDamage(-4.0);
        verify(event).setCancelled(true);
    }

    @Test
    public void onVisitorDamageByEntity_allowsPlayerPvPWhenPvPEnabled() {
        useConfig(true, false, false, true, false, "allow");
        Player victim = mock(Player.class);
        when(victim.getWorld()).thenReturn(mock(World.class));
        when(plugin.playerIsOnIsland(victim)).thenReturn(false);
        Player attacker = mock(Player.class);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(attacker);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);

        listener.onVisitorDamageByEntity(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    public void onMemberDamage_cancelsPvPBetweenSameIslandMembers() {
        useConfig(true, false, false, false, false, "allow");
        Player victim = mock(Player.class);
        when(victim.getWorld()).thenReturn(mock(World.class));
        Player attacker = mock(Player.class);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(attacker);

        IslandInfo island = mock(IslandInfo.class);
        when(island.getName()).thenReturn("island-alpha");
        when(plugin.getIslandInfo(attacker)).thenReturn(island);
        when(plugin.getIslandInfo(victim)).thenReturn(island);

        listener.onMemberDamage(event);

        verify(event).setCancelled(true);
        verify(plugin).notifyPlayer(eq(attacker), any());
    }

    @Test
    public void onMemberDamage_allowsPvPBetweenDifferentIslands() {
        useConfig(true, false, false, false, false, "allow");
        Player victim = mock(Player.class);
        when(victim.getWorld()).thenReturn(mock(World.class));
        Player attacker = mock(Player.class);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(attacker);

        IslandInfo attackerIsland = mock(IslandInfo.class);
        when(attackerIsland.getName()).thenReturn("island-alpha");
        IslandInfo victimIsland = mock(IslandInfo.class);
        when(victimIsland.getName()).thenReturn("island-beta");
        when(plugin.getIslandInfo(attacker)).thenReturn(attackerIsland);
        when(plugin.getIslandInfo(victim)).thenReturn(victimIsland);

        listener.onMemberDamage(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    public void onLavaReplace_cancelsReplacingLavaSource() {
        useConfig(true, false, false, false, true, "deny");
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(mock(World.class));

        Levelled lava = lavaSource();
        BlockState replaced = mock(BlockState.class);
        when(replaced.getBlockData()).thenReturn(lava);

        BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlockReplacedState()).thenReturn(replaced);

        listener.onLavaReplace(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void onLavaAbsorption_cancelsEntityConsumingLavaSource() {
        useConfig(true, false, false, false, true, "deny");
        World world = mock(World.class);
        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        Levelled lava = lavaSource();
        when(block.getBlockData()).thenReturn(lava);

        EntityChangeBlockEvent event = mock(EntityChangeBlockEvent.class);
        when(event.getBlock()).thenReturn(block);
        when(event.getTo()).thenReturn(Material.COBBLESTONE);

        listener.onLavaAbsorption(event);

        verify(event).setCancelled(true);
        verify(world).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }

    private Levelled lavaSource() {
        Levelled lava = mock(Levelled.class);
        when(lava.getMaterial()).thenReturn(Material.LAVA);
        when(lava.getLevel()).thenReturn(0);
        return lava;
    }

    private void useConfig(boolean protectionEnabled, boolean fireDamage, boolean fallDamage,
                           boolean monsterDamage, boolean protectLava, String allowPvP) {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("options.protection.enabled", protectionEnabled);
        config.set("options.protection.visitors.fire-damage", fireDamage);
        config.set("options.protection.visitors.fall", fallDamage);
        config.set("options.protection.visitors.monster-damage", monsterDamage);
        config.set("options.protection.protect-lava", protectLava);
        config.set("options.island.allowPvP", allowPvP);
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);
        doReturn(runtimeConfig).when(runtimeConfigs).current();
    }
}
