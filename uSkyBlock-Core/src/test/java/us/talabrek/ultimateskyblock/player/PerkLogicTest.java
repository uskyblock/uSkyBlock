package us.talabrek.ultimateskyblock.player;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.handler.SchematicHandler;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PerkLogicTest {
    private PerkLogic perkLogic;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        YamlConfiguration config = PluginConfigLoader.loadBundledConfig();
        // Item perks need a live item registry.
        clearItemLists(config);
        config.set("donor-perks.usb.donor.ghasts.happy-ghasts", 8);
        config.set("island-schemes.default.happy-ghasts", 6);
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger())
            .load(config);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);
        SchematicHandler schematicHandler = mock(SchematicHandler.class);
        when(schematicHandler.getSchemeNames()).thenReturn(List.of("default"));
        perkLogic = new PerkLogic(schematicHandler, runtimeConfigs);
    }

    private static void clearItemLists(YamlConfiguration config) {
        for (String path : config.getKeys(true)) {
            if (path.endsWith(".extraItems") || path.startsWith("options.island.extraPermissions.")) {
                config.set(path, List.of());
            }
        }
    }

    @Test
    public void happyGhastLimitDefaultsToFour() {
        assertEquals(4, perkLogic.getDefaultPerk().getHappyGhasts());
    }

    @Test
    public void donorPerkRaisesHappyGhastLimit() {
        Player player = mock(Player.class);
        when(player.hasPermission("usb.donor.ghasts")).thenReturn(true);

        assertEquals(8, perkLogic.getPerk(player).getHappyGhasts());
    }

    @Test
    public void islandSchemeSetsHappyGhastLimit() {
        assertEquals(6, perkLogic.getIslandPerk("default").getPerk().getHappyGhasts());
    }
}
