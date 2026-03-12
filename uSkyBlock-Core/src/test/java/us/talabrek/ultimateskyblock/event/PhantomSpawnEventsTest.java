package us.talabrek.ultimateskyblock.event;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.PluginConfig;
import us.talabrek.ultimateskyblock.world.WorldManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PhantomSpawnEventsTest {
    private PhantomSpawnEvents phantomSpawnEvents;
    private WorldManager worldManager;

    @BeforeEach
    public void setUp() {
        YamlConfiguration config = new YamlConfiguration();
        PluginConfig pluginConfig = mock(PluginConfig.class);
        worldManager = mock(WorldManager.class);
        when(pluginConfig.getYamlConfig()).thenReturn(config);
        phantomSpawnEvents = new PhantomSpawnEvents(pluginConfig, worldManager);
    }

    @Test
    public void onPhantomSpawn_noPhantom() {
        Zombie entity = mock(Zombie.class);
        CreatureSpawnEvent event = new CreatureSpawnEvent(entity, CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_overworldAllowed() {
        when(worldManager.isSkyWorld(any(World.class))).thenReturn(true);

        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.setPhantomsInOverworld(true);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_overworldNotAllowed() {
        when(worldManager.isSkyWorld(any(World.class))).thenReturn(true);

        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.setPhantomsInOverworld(false);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertTrue(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_notInSkyworld() {
        when(worldManager.isSkyWorld(any(World.class))).thenReturn(false);

        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.setPhantomsInOverworld(false);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_notNaturalSpawned() {
        when(worldManager.isSkyWorld(any(World.class))).thenReturn(true);

        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
        phantomSpawnEvents.setPhantomsInOverworld(false);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_netherAllowed() {
        when(worldManager.isSkyNether(any(World.class))).thenReturn(true);

        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.setPhantomsInNether(true);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_netherNotAllowed() {
        when(worldManager.isSkyNether(any(World.class))).thenReturn(true);

        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.setPhantomsInNether(false);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertTrue(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_notInNetherWorld() {
        when(worldManager.isSkyNether(any(World.class))).thenReturn(false);

        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.setPhantomsInNether(false);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertFalse(event.isCancelled());
    }

    private Phantom getFakePhantom() {
        World fakeWorld = mock(World.class);
        when(fakeWorld.getName()).thenReturn("skyworld");
        Phantom entity = mock(Phantom.class);
        when(entity.getWorld()).thenReturn(fakeWorld);

        return entity;
    }
}
