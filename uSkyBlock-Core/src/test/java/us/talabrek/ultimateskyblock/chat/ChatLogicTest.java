package us.talabrek.ultimateskyblock.chat;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.api.event.IslandChatEvent;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;
import us.talabrek.ultimateskyblock.handler.placeholder.PlaceholderHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChatLogicTest {
    private final AtomicReference<Component> delivered = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Msg.configure((sender, message) -> delivered.set(message));
    }

    @AfterEach
    void tearDown() {
        Msg.configure(null);
    }

    @Test
    void sendsMiniMessageFormattedPartyChat() {
        uSkyBlock plugin = mock(uSkyBlock.class);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        WorldManager worldManager = mock(WorldManager.class);
        PlaceholderHandler placeholderHandler = mock(PlaceholderHandler.class);
        Player sender = mock(Player.class);
        Player recipient = mock(Player.class);
        IslandInfo islandInfo = mock(IslandInfo.class);
        RuntimeConfig runtimeConfig = runtimeConfig();

        when(runtimeConfigs.current()).thenReturn(runtimeConfig);
        when(plugin.getIslandInfo(sender)).thenReturn(islandInfo);
        when(islandInfo.getOnlineMembers()).thenReturn(List.of(sender, recipient));
        when(sender.getDisplayName()).thenReturn("§cMinoneer");
        when(placeholderHandler.replacePlaceholders(eq(sender), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(1));

        ChatLogic chatLogic = new ChatLogic(plugin, runtimeConfigs, worldManager, placeholderHandler);
        chatLogic.sendMessage(sender, IslandChatEvent.Type.PARTY, "hello <green>tag");

        assertThat(I18nUtil.legacy(delivered.get()), is("§9PARTY §cMinoneer§r §f>§b hello <green>tag"));
    }

    private RuntimeConfig runtimeConfig() {
        return new RuntimeConfig(
            "en",
            Locale.ENGLISH,
            new RuntimeConfig.Init(Duration.ofMillis(2500)),
            new RuntimeConfig.General(
                4, "skyworld", Duration.ofSeconds(20), Duration.ofSeconds(30), Duration.ofSeconds(60),
                "ocean", "nether_wastes", 64, Duration.ofSeconds(2)
            ),
            new RuntimeConfig.Island(
                128, 150, false, 128, 64, List.of(), true, Map.of(), true, true, true,
                "default", Duration.ofMinutes(20), Duration.ofMinutes(5), false, Duration.ofSeconds(2),
                0.3d, Duration.ZERO, true, 10, true,
                "<blue>SKY </blue><display-name> <white>></white><light_purple> <message>",
                new RuntimeConfig.SpawnLimits(true, 64, 50, 16, 5, 5), Map.of()
            ),
            new RuntimeConfig.Extras(true, false, true),
            new RuntimeConfig.Protection(
                true, true, true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, true, true, true, true
            ),
            new RuntimeConfig.Nether(
                false, 0, 150, "bukkit",
                new RuntimeConfig.Terraform(false, -0.5d, 0.5d, 16, Map.of(), Map.of()),
                new RuntimeConfig.SpawnChances(false, 0.0d, 0.0d, 0.0d)
            ),
            new RuntimeConfig.Restart(true, true, true, true, true, true, Duration.ofSeconds(1), List.of()),
            new RuntimeConfig.Advanced(
                Duration.ofSeconds(10), true, 0.0d, true,
                "maximumSize=1000", "maximumSize=1000", "maximumSize=1000", "maximumSize=1000",
                Duration.ofSeconds(30), Duration.ofSeconds(120), "bukkit", 4,
                Duration.ofSeconds(30), 50.0d, Duration.ofMinutes(10), null,
                new RuntimeConfig.PlayerDb("yaml", "maximumSize=1000", "maximumSize=1000", Duration.ofSeconds(10))
            ),
            new RuntimeConfig.Async(Duration.ofMillis(15), 20L, Duration.ofMillis(100)),
            new RuntimeConfig.AsyncWorldEdit(false, Duration.ofSeconds(2), Duration.ofSeconds(30)),
            new RuntimeConfig.Party(
                Duration.ofMinutes(2),
                "<blue>PARTY </blue><display-name> <white>></white><aqua> <message>",
                List.of(),
                List.of(),
                Map.of()
            ),
            new RuntimeConfig.PluginUpdates(false, "master"),
            new RuntimeConfig.Spawning(
                new RuntimeConfig.Guardians(true, 10, 0.1d),
                new RuntimeConfig.Phantoms(true, false)
            ),
            new RuntimeConfig.Placeholder(true, false, false),
            new RuntimeConfig.ToolMenu(true, new ItemStackSpec(new ItemStack(Material.STICK)), List.of()),
            new RuntimeConfig.Signs(true),
            new RuntimeConfig.WorldGuard(false, false),
            new RuntimeConfig.Importer(10.0d, Duration.ofSeconds(10)),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of()
        );
    }
}
