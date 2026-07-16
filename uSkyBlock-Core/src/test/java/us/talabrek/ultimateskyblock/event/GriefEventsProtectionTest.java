package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.Arrays;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the mockable, environmental decisions in {@link GriefEvents}.
 *
 * <p>The wither-rampage branches ({@code onTargeting} / {@code onWitherSkullExplosion}) are not
 * covered here: they depend on {@code WorldGuardHandler.getIslandNameAt(...)} cross-island geometry,
 * which is not available on the unit-test classpath. Those are left for the live-server harness.
 */
public class GriefEventsProtectionTest {

    private uSkyBlock plugin;
    private WorldManager worldManager;
    private GriefEvents griefEvents;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        plugin = mock(uSkyBlock.class);

        // Load a REAL RuntimeConfig from the bundled defaults; all protection flags default to true.
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("options.party.join-commands", Arrays.asList("lets", "test", "this"));
        config.set("options.party.leave-commands", Arrays.asList("dont", "stop", "me", "now"));
        config.set("language", "en");
        config.set("options.general.worldName", "skyworld");
        config.set("options.general.cooldownRestart", "1m");
        config.set("options.general.biomeChange", "1m");
        config.set("options.general.defaultBiome", "plains");
        config.set("options.general.defaultNetherBiome", "nether_wastes");
        config.set("options.advanced.confirmTimeout", "30s");
        config.set("options.advanced.playerdb.storage", "file");
        config.set("options.advanced.playerdb.nameCache", "maximumSize=100");
        config.set("options.advanced.playerdb.uuidCache", "maximumSize=100");
        config.set("options.advanced.playerCache", "maximumSize=100");
        config.set("options.advanced.islandCache", "maximumSize=100");
        config.set("options.advanced.island.saveEvery", 60);
        config.set("options.advanced.player.saveEvery", 60);
        config.set("options.party.invite-timeout", "1m");
        config.set("options.island.distance", 110);
        config.set("options.island.height", 120);
        config.set("options.island.topTenTimeout", "15m");
        config.set("options.island.islandTeleportDelay", "2s");
        config.set("options.island.chat-format", "default");
        config.set("options.party.chat-format", "default");
        config.set("nether.chunk-generator", "default");
        config.set("plugin-updates.branch", "LATEST");

        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        doReturn(new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config))
            .when(runtimeConfigs).current();

        griefEvents = new GriefEvents(plugin, runtimeConfigs);

        // Every handler first checks that the event happens in a sky-associated world.
        worldManager = mock(WorldManager.class);
        doReturn(true).when(worldManager).isSkyAssociatedWorld(any());
        doReturn(worldManager).when(plugin).getWorldManager();
    }

    @Test
    public void creeperTargetingVisitorIsCancelled() {
        Player visitor = mock(Player.class);
        doReturn(false).when(plugin).playerIsOnIsland(visitor);

        Creeper creeper = mock(Creeper.class);
        doReturn(visitor).when(creeper).getTarget();

        ExplosionPrimeEvent event = mock(ExplosionPrimeEvent.class);
        doReturn(creeper).when(event).getEntity();

        griefEvents.onCreeperExplode(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void creeperTargetingIslandPlayerIsNotCancelled() {
        Player member = mock(Player.class);
        doReturn(true).when(plugin).playerIsOnIsland(member);

        Creeper creeper = mock(Creeper.class);
        doReturn(member).when(creeper).getTarget();

        ExplosionPrimeEvent event = mock(ExplosionPrimeEvent.class);
        doReturn(creeper).when(event).getEntity();

        griefEvents.onCreeperExplode(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    public void tntPrimedByVisitorIsCancelled() {
        Player visitor = mock(Player.class);
        doReturn(false).when(plugin).playerIsOnIsland(visitor);

        TNTPrimed tnt = mock(TNTPrimed.class);
        doReturn(visitor).when(tnt).getSource();

        ExplosionPrimeEvent event = mock(ExplosionPrimeEvent.class);
        doReturn(tnt).when(event).getEntity();

        griefEvents.onCreeperExplode(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void shearingByVisitorIsCancelled() {
        Player visitor = mock(Player.class);
        doReturn(false).when(plugin).playerIsOnIsland(visitor);

        PlayerShearEntityEvent event = mock(PlayerShearEntityEvent.class);
        doReturn(visitor).when(event).getPlayer();

        griefEvents.onShearEvent(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void shearingByIslandPlayerIsNotCancelled() {
        Player member = mock(Player.class);
        doReturn(true).when(plugin).playerIsOnIsland(member);

        PlayerShearEntityEvent event = mock(PlayerShearEntityEvent.class);
        doReturn(member).when(event).getPlayer();

        griefEvents.onShearEvent(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    public void tramplingFarmlandByVisitorIsCancelled() {
        Player visitor = mock(Player.class);
        doReturn(false).when(plugin).playerIsOnIsland(visitor);

        Block block = mock(Block.class);
        doReturn(Material.FARMLAND).when(block).getType();

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        doReturn(visitor).when(event).getPlayer();
        doReturn(Action.PHYSICAL).when(event).getAction();
        doReturn(true).when(event).hasBlock();
        doReturn(block).when(event).getClickedBlock();

        griefEvents.onTrampling(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void eggThrowByVisitorDoesNotHatch() {
        Player visitor = mock(Player.class);
        doReturn(false).when(plugin).playerIsOnIsland(visitor);

        PlayerEggThrowEvent event = mock(PlayerEggThrowEvent.class);
        doReturn(visitor).when(event).getPlayer();

        griefEvents.onEgg(event);

        verify(event).setHatching(false);
    }

    @Test
    public void eggThrowByIslandPlayerHatchesNormally() {
        Player member = mock(Player.class);
        doReturn(true).when(plugin).playerIsOnIsland(member);

        PlayerEggThrowEvent event = mock(PlayerEggThrowEvent.class);
        doReturn(member).when(event).getPlayer();

        griefEvents.onEgg(event);

        verify(event, never()).setHatching(anyBoolean());
    }

    @Test
    public void killAnimalByVisitorIsCancelled() {
        Player visitor = mock(Player.class);
        doReturn(false).when(plugin).playerIsOnIsland(visitor);

        Creature animal = mock(Creature.class);
        LimitLogic limitLogic = mock(LimitLogic.class);
        doReturn(LimitLogic.CreatureType.ANIMAL).when(limitLogic).getCreatureType(animal);
        doReturn(limitLogic).when(plugin).getLimitLogic();

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        doReturn(visitor).when(event).getDamager();
        doReturn(animal).when(event).getEntity();

        griefEvents.onEntityDamage(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void killMonsterByVisitorIsCancelled() {
        Player visitor = mock(Player.class);
        doReturn(false).when(plugin).playerIsOnIsland(visitor);

        Creature monster = mock(Creature.class);
        LimitLogic limitLogic = mock(LimitLogic.class);
        doReturn(LimitLogic.CreatureType.MONSTER).when(limitLogic).getCreatureType(monster);
        doReturn(limitLogic).when(plugin).getLimitLogic();

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        doReturn(visitor).when(event).getDamager();
        doReturn(monster).when(event).getEntity();

        griefEvents.onEntityDamage(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void damageByIslandPlayerIsNotCancelled() {
        Player member = mock(Player.class);
        doReturn(true).when(plugin).playerIsOnIsland(member);

        Creature animal = mock(Creature.class);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        doReturn(member).when(event).getDamager();
        doReturn(animal).when(event).getEntity();

        griefEvents.onEntityDamage(event);

        verify(event, never()).setCancelled(true);
    }
}
