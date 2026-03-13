package us.talabrek.ultimateskyblock.event;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.world.WorldManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PhantomSpawnEventsTest {
    private final WorldManager worldManager = mock(WorldManager.class);

    @Test
    public void onPhantomSpawn_noPhantom() {
        PhantomSpawnEvents phantomSpawnEvents = new PhantomSpawnEvents(runtimeConfigs(true, false), worldManager);
        Zombie entity = mock(Zombie.class);
        CreatureSpawnEvent event = new CreatureSpawnEvent(entity, CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_overworldAllowed() {
        when(worldManager.isSkyWorld(any(World.class))).thenReturn(true);

        PhantomSpawnEvents phantomSpawnEvents = new PhantomSpawnEvents(runtimeConfigs(true, false), worldManager);
        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_overworldNotAllowed() {
        when(worldManager.isSkyWorld(any(World.class))).thenReturn(true);

        PhantomSpawnEvents phantomSpawnEvents = new PhantomSpawnEvents(runtimeConfigs(false, false), worldManager);
        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertTrue(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_notInSkyworld() {
        when(worldManager.isSkyWorld(any(World.class))).thenReturn(false);

        PhantomSpawnEvents phantomSpawnEvents = new PhantomSpawnEvents(runtimeConfigs(false, false), worldManager);
        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_notNaturalSpawned() {
        when(worldManager.isSkyWorld(any(World.class))).thenReturn(true);

        PhantomSpawnEvents phantomSpawnEvents = new PhantomSpawnEvents(runtimeConfigs(false, false), worldManager);
        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_netherAllowed() {
        when(worldManager.isSkyNether(any(World.class))).thenReturn(true);

        PhantomSpawnEvents phantomSpawnEvents = new PhantomSpawnEvents(runtimeConfigs(true, true), worldManager);
        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_netherNotAllowed() {
        when(worldManager.isSkyNether(any(World.class))).thenReturn(true);

        PhantomSpawnEvents phantomSpawnEvents = new PhantomSpawnEvents(runtimeConfigs(true, false), worldManager);
        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
        phantomSpawnEvents.onPhantomSpawn(event);

        assertTrue(event.isCancelled());
    }

    @Test
    public void onPhantomSpawn_notInNetherWorld() {
        when(worldManager.isSkyNether(any(World.class))).thenReturn(false);

        PhantomSpawnEvents phantomSpawnEvents = new PhantomSpawnEvents(runtimeConfigs(true, false), worldManager);
        CreatureSpawnEvent event = new CreatureSpawnEvent(getFakePhantom(),
            CreatureSpawnEvent.SpawnReason.NATURAL);
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

    private RuntimeConfigs runtimeConfigs(boolean overworld, boolean nether) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("options.spawning.phantoms.overworld", overworld);
        config.set("options.spawning.phantoms.nether", nether);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(RuntimeConfigFactory.load(config));
        return runtimeConfigs;
    }
}
