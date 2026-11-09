package us.talabrek.ultimateskyblock.handler;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfirmHandlerTest {

    @AfterEach
    public void resetMessages() {
        Msg.configure(null);
    }

    @Test
    public void consoleConfirmsByRepeatingTheCommand() throws Exception {
        BukkitServerMock.setupServerMock();
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger())
            .load(config);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        List<String> messages = new ArrayList<>();
        Msg.configure((sender, message) -> messages.add(Msg.plainText(message)));
        ConfirmHandler confirmHandler = new ConfirmHandler(mock(Scheduler.class), runtimeConfigs);
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);

        assertFalse(confirmHandler.checkCommand(console, "/usb update"));
        assertTrue(confirmHandler.checkCommand(console, "/usb update"));
        assertFalse(confirmHandler.checkCommand(console, "/usb update"));

        assertEquals("Running /usb update is risky. Repeat it within 10 seconds to confirm.", messages.getFirst());
    }
}
