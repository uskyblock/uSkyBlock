package us.talabrek.ultimateskyblock.menu;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.handler.ConfirmHandler;
import us.talabrek.ultimateskyblock.handler.SchematicHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.player.PerkLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.UltimateHolder;
import us.talabrek.ultimateskyblock.player.UltimateHolder.MenuType;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SkyBlockMenuTest {
    private final uSkyBlock plugin = mock(uSkyBlock.class);
    private final ChallengeLogic challengeLogic = mock(ChallengeLogic.class);
    private final RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
    private final ConfirmHandler confirmHandler = mock(ConfirmHandler.class);
    private final PerkLogic perkLogic = mock(PerkLogic.class);
    private final Scheduler scheduler = mock(Scheduler.class);
    private final Player player = mock(Player.class);
    private final IslandInfo islandInfo = mock(IslandInfo.class);
    private final YamlConfiguration config = new YamlConfiguration();
    private final List<String> sent = new ArrayList<>();
    private Server server;
    private SkyBlockMenu menu;

    @BeforeEach
    public void setUp() throws Exception {
        server = BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Msg.configure((sender, message) -> sent.add(Msg.plainText(message)));
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        loadConfig();
        BukkitServerMock.useMetaData = true;
        when(player.hasPermission(anyString())).thenReturn(true);
        PlayerInfo playerInfo = mock(PlayerInfo.class);
        when(plugin.getPlayerInfo(player)).thenReturn(playerInfo);
        when(plugin.getIslandInfo(playerInfo)).thenReturn(islandInfo);
        when(plugin.getIslandInfo(player)).thenReturn(islandInfo);
        when(islandInfo.getBiomeName()).thenReturn("ocean");
        when(confirmHandler.durationLeft(player, "/is leave")).thenReturn(Duration.ZERO);
        menu = new SkyBlockMenu(plugin, challengeLogic, runtimeConfigs, perkLogic, mock(SchematicHandler.class),
            mock(LimitLogic.class), confirmHandler, scheduler);
    }

    private void loadConfig() {
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);
    }

    @AfterEach
    public void tearDown() {
        BukkitServerMock.useMetaData = false;
        Msg.configure(null);
    }

    @Test
    public void mainMenuMarksIconsWithoutPermissionAsUnavailable() {
        when(plugin.hasIsland(player)).thenReturn(true);
        when(player.hasPermission("usb.island.home")).thenReturn(false);
        List<ItemStack> items = captureMenuItems();

        menu.displayIslandGUI(player);

        assertThat(loreOf(items, Material.OAK_DOOR), containsString("You do not have permission for this."));
        assertThat(loreOf(items, Material.OAK_DOOR), not(containsString("Click here to return home.")));
        assertThat(loreOf(items, Material.SHORT_GRASS), not(containsString("You do not have permission for this.")));
        assertThat(loreOf(items, Material.WRITABLE_BOOK), containsString("Click to view the log."));
    }

    @Test
    public void clickOnMainMenuIconWithoutPermissionOnlySendsReason() {
        when(player.hasPermission("usb.island.home")).thenReturn(false);
        InventoryClickEvent event = click(trLegacy("Island Menu"), 18, 0, Material.OAK_DOOR);

        menu.onClick(event);

        verify(event).setCancelled(true);
        verify(player, never()).performCommand(anyString());
        assertThat(sent, contains("You do not have permission for this."));
    }

    @Test
    public void clickOnMainMenuIconWithPermissionRunsCommand() {
        InventoryClickEvent event = click(trLegacy("Island Menu"), 18, 0, Material.OAK_DOOR);

        menu.onClick(event);

        verify(player).performCommand("island home");
    }

    @Test
    public void leaveWithoutPermissionIsUnavailableAndOnlySendsReason() {
        when(plugin.hasIsland(player)).thenReturn(true);
        when(player.hasPermission("usb.party.leave")).thenReturn(false);
        when(confirmHandler.durationLeft(player, "/is leave")).thenReturn(Duration.ofSeconds(5));
        List<ItemStack> items = captureMenuItems();

        menu.displayIslandGUI(player);
        menu.onClick(click(trLegacy("Island Menu"), 18, 17, Material.IRON_DOOR));

        assertThat(loreOf(items, Material.IRON_DOOR), containsString("You do not have permission for this."));
        verify(player, never()).performCommand(anyString());
        verify(player, never()).openInventory(nullable(Inventory.class));
        verifyNoInteractions(scheduler);
        assertThat(sent, contains("You do not have permission for this."));
    }

    @Test
    public void restartWithoutPermissionOnlySendsReason() {
        when(islandInfo.isLeader(player)).thenReturn(true);
        when(player.hasPermission("usb.island.restart")).thenReturn(false);

        menu.onClick(click(trLegacy("Island Menu"), 18, 17, Material.PODZOL));

        verify(player, never()).openInventory(nullable(Inventory.class));
        assertThat(sent, contains("You do not have permission for this."));
    }

    @Test
    public void clickOnIconWithoutIslandPermissionOnlySendsReason() {
        InventoryClickEvent event = click(trLegacy("Island Menu"), 18, 5, Material.IRON_BARS);

        menu.onClick(event);

        verify(player, never()).performCommand(anyString());
        assertThat(sent, contains("You can't change the lock."));
    }

    @Test
    public void lockIsUnavailableWhenIslandLockIsDisabled() {
        config.set("options.island.allowIslandLock", false);
        loadConfig();
        when(plugin.hasIsland(player)).thenReturn(true);
        when(islandInfo.hasPerm(player, "canToggleLock")).thenReturn(true);
        List<ItemStack> items = captureMenuItems();

        menu.displayIslandGUI(player);
        menu.onClick(click(trLegacy("Island Menu"), 18, 5, Material.IRON_BARS));

        assertThat(loreOf(items, Material.IRON_BARS), containsString("You can't change the lock."));
        verify(player, never()).performCommand(anyString());
        assertThat(sent, contains("You can't change the lock."));
    }

    @Test
    public void extraMenuItemWithRestartIconRunsItsCommandsWithoutRestartPermission() {
        config.set("options.extra-menus.13.title", "Shop");
        config.set("options.extra-menus.13.displayItem", "PODZOL");
        config.set("options.extra-menus.13.commands", List.of("shop"));
        loadConfig();
        when(player.hasPermission("usb.island.restart")).thenReturn(false);
        InventoryClickEvent event = click(trLegacy("Island Menu"), 18, 13, Material.PODZOL);
        when(event.getCurrentItem().getItemMeta().getDisplayName()).thenReturn("Shop");

        menu.onClick(event);

        verify(plugin).execCommand(player, "shop", false);
    }

    @Test
    public void clickOnCreateMenuIconWithoutPermissionOnlySendsReason() {
        when(player.hasPermission("usb.party.join")).thenReturn(false);
        InventoryClickEvent event = click(trLegacy("Island Create Menu"), 9, 8, Material.PLAYER_HEAD);

        menu.onClick(event);

        verify(event).setCancelled(true);
        verify(player, never()).performCommand(anyString());
        assertThat(sent, contains("You do not have permission for this."));
    }

    @Test
    public void clickOnCreateMenuIconWithPermissionRunsCommand() {
        InventoryClickEvent event = click(trLegacy("Island Create Menu"), 9, 8, Material.PLAYER_HEAD);

        menu.onClick(event);

        verify(player).performCommand("island accept");
    }

    @Test
    public void clickOnSchemeWithoutAccessOnlySendsReason() {
        InventoryClickEvent event = click(trLegacy("Island Create Menu"), 9, 1, Material.OAK_SAPLING);
        ItemMeta meta = event.getCurrentItem().getItemMeta();
        when(meta.hasDisplayName()).thenReturn(true);
        when(meta.getDisplayName()).thenReturn("skySMP");

        menu.onClick(event);

        verify(player, never()).performCommand(anyString());
        assertThat(sent, contains("You do not have access to that island schematic."));
    }

    private List<ItemStack> captureMenuItems() {
        List<ItemStack> items = new ArrayList<>();
        Inventory inventory = mock(Inventory.class);
        doAnswer(invocation -> {
            for (Object item : invocation.getArguments()) {
                items.add((ItemStack) item);
            }
            return new HashMap<>();
        }).when(inventory).addItem(any(ItemStack[].class));
        doAnswer(invocation -> items.add(invocation.getArgument(1))).when(inventory).setItem(anyInt(), any());
        when(server.createInventory(any(InventoryHolder.class), anyInt(), anyString())).thenReturn(inventory);
        return items;
    }

    private static String loreOf(List<ItemStack> items, Material type) {
        ItemStack item = items.stream().filter(i -> i.getType() == type).findFirst().orElseThrow();
        return String.valueOf(item.getItemMeta().getLore());
    }

    private InventoryClickEvent click(String menuTitle, int menuSize, int slot, Material type) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        when(item.getItemMeta()).thenReturn(mock(ItemMeta.class));
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(new UltimateHolder(player, menuTitle, MenuType.DEFAULT));
        when(inventory.getSize()).thenReturn(menuSize);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getCurrentItem()).thenReturn(item);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getSlot()).thenReturn(slot);
        return event;
    }
}
