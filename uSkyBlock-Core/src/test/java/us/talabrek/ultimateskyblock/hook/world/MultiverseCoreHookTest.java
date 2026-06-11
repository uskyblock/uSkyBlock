package us.talabrek.ultimateskyblock.hook.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mvplugins.multiverse.core.MultiverseCore;
import org.mvplugins.multiverse.core.config.handle.StringPropertyHandle;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.external.vavr.control.Try;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MultiverseCoreHookTest {

    private MultiverseCoreHook hook;
    private TestLogHandler logHandler;
    private WorldManager mvWorldManager;

    @BeforeEach
    public void setUp() {
        uSkyBlock plugin = mock(uSkyBlock.class, RETURNS_DEEP_STUBS);
        when(plugin.getServer().getPluginManager().getPlugin("Multiverse-Core"))
            .thenReturn(mock(MultiverseCore.class));

        logHandler = new TestLogHandler();
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(logHandler);
        when(plugin.getLogger()).thenReturn(logger);

        hook = new MultiverseCoreHook(plugin, mock(RuntimeConfigs.class));
        mvWorldManager = mock(WorldManager.class);
        when(mvWorldManager.saveWorldsConfig()).thenReturn(Try.success(null));
    }

    @Test
    public void setsGeneratorWhenMissing() {
        MultiverseWorld mvWorld = mock(MultiverseWorld.class);
        StringPropertyHandle propertyHandle = mock(StringPropertyHandle.class);
        when(mvWorld.getName()).thenReturn("skyworld");
        when(mvWorld.getGenerator()).thenReturn(null);
        when(mvWorld.getStringPropertyHandle()).thenReturn(propertyHandle);
        when(propertyHandle.setProperty("generator", "uSkyBlock")).thenReturn(Try.success(null));

        hook.ensureGeneratorRegistered(mvWorldManager, mvWorld);

        verify(propertyHandle).setProperty("generator", "uSkyBlock");
        verify(mvWorldManager).saveWorldsConfig();
    }

    @Test
    public void setsGeneratorWhenEmpty() {
        MultiverseWorld mvWorld = mock(MultiverseWorld.class);
        StringPropertyHandle propertyHandle = mock(StringPropertyHandle.class);
        when(mvWorld.getName()).thenReturn("skyworld");
        when(mvWorld.getGenerator()).thenReturn("");
        when(mvWorld.getStringPropertyHandle()).thenReturn(propertyHandle);
        when(propertyHandle.setProperty("generator", "uSkyBlock")).thenReturn(Try.success(null));

        hook.ensureGeneratorRegistered(mvWorldManager, mvWorld);

        verify(propertyHandle).setProperty("generator", "uSkyBlock");
        verify(mvWorldManager).saveWorldsConfig();
    }

    @Test
    public void leavesMatchingGeneratorUntouched() {
        MultiverseWorld mvWorld = mock(MultiverseWorld.class);
        when(mvWorld.getGenerator()).thenReturn("uSkyBlock");

        hook.ensureGeneratorRegistered(mvWorldManager, mvWorld);

        verify(mvWorld, never()).getStringPropertyHandle();
        verify(mvWorldManager, never()).saveWorldsConfig();
        assertThat(logHandler.warnings(), is(emptyString()));
    }

    @Test
    public void warnsButKeepsForeignGenerator() {
        MultiverseWorld mvWorld = mock(MultiverseWorld.class);
        when(mvWorld.getName()).thenReturn("skyworld");
        when(mvWorld.getGenerator()).thenReturn("VoidGen");

        hook.ensureGeneratorRegistered(mvWorldManager, mvWorld);

        verify(mvWorld, never()).getStringPropertyHandle();
        assertThat(logHandler.warnings(), containsString("VoidGen"));
    }

    @Test
    public void logsSevereAndSkipsSaveWhenSetPropertyFails() {
        MultiverseWorld mvWorld = mock(MultiverseWorld.class);
        StringPropertyHandle propertyHandle = mock(StringPropertyHandle.class);
        when(mvWorld.getName()).thenReturn("skyworld");
        when(mvWorld.getGenerator()).thenReturn(null);
        when(mvWorld.getStringPropertyHandle()).thenReturn(propertyHandle);
        when(propertyHandle.setProperty("generator", "uSkyBlock"))
            .thenReturn(Try.failure(new RuntimeException("boom")));

        hook.ensureGeneratorRegistered(mvWorldManager, mvWorld);

        verify(mvWorldManager, never()).saveWorldsConfig();
        assertThat(logHandler.severeMessages(), containsString("skyworld"));
    }

    private static final class TestLogHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        String warnings() {
            return records.stream()
                .filter(record -> record.getLevel() == Level.WARNING)
                .map(LogRecord::getMessage)
                .collect(Collectors.joining("\n"));
        }

        String severeMessages() {
            return records.stream()
                .filter(record -> record.getLevel() == Level.SEVERE)
                .map(LogRecord::getMessage)
                .collect(Collectors.joining("\n"));
        }
    }
}
