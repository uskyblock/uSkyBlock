package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ItemDropEventsTest {
    private uSkyBlock plugin;
    private WorldManager worldManager;
    private ItemDropEvents itemDropEvents;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);

        plugin = mock(uSkyBlock.class);
        worldManager = mock(WorldManager.class);
        doReturn(worldManager).when(plugin).getWorldManager();

        // visitors are NOT allowed to drop items
        itemDropEvents = new ItemDropEvents(plugin, runtimeConfigs(false));
    }

    @Test
    public void dropTagsItemWithOwnerLoreForResident() {
        World world = mock(World.class);
        Player player = mock(Player.class);
        doReturn(world).when(player).getWorld();
        doReturn("Notch").when(player).getName();

        ItemMeta meta = mock(ItemMeta.class);
        doReturn(null).when(meta).getLore();
        ItemStack stack = mock(ItemStack.class);
        doReturn(meta).when(stack).getItemMeta();
        Item itemEntity = mock(Item.class);
        doReturn(stack).when(itemEntity).getItemStack();

        PlayerDropItemEvent event = mock(PlayerDropItemEvent.class);
        doReturn(player).when(event).getPlayer();
        doReturn(itemEntity).when(event).getItemDrop();

        doReturn(true).when(worldManager).isSkyAssociatedWorld(world);
        doReturn(true).when(plugin).playerIsOnIsland(player);
        doReturn(false).when(plugin).playerIsInSpawn(player);

        itemDropEvents.onDropEvent(event);

        String ownerTag = I18nUtil.trLegacy("Owner: <player>", unparsed("player", "Notch"));
        ArgumentCaptor<List> loreCaptor = ArgumentCaptor.forClass(List.class);
        verify(meta).setLore(loreCaptor.capture());
        assertTrue(loreCaptor.getValue().contains(ownerTag));
        verify(stack).setItemMeta(meta);
        verify(event, never()).setCancelled(true);
    }

    @Test
    public void dropIsBlockedForVisitor() {
        World world = mock(World.class);
        Player player = mock(Player.class);
        doReturn(world).when(player).getWorld();

        PlayerDropItemEvent event = mock(PlayerDropItemEvent.class);
        doReturn(player).when(event).getPlayer();

        doReturn(true).when(worldManager).isSkyAssociatedWorld(world);
        doReturn(false).when(plugin).playerIsOnIsland(player);
        doReturn(false).when(plugin).playerIsInSpawn(player);

        itemDropEvents.onDropEvent(event);

        verify(event).setCancelled(true);
        verify(plugin).notifyPlayer(eq(player), any());
        verify(event, never()).getItemDrop();
    }

    @Test
    public void deathKeepsInventoryForVisitor() {
        World world = mock(World.class);
        Player player = mock(Player.class);
        doReturn(world).when(player).getWorld();

        PlayerDeathEvent event = mock(PlayerDeathEvent.class);
        doReturn(player).when(event).getEntity();

        doReturn(true).when(worldManager).isSkyAssociatedWorld(world);
        doReturn(false).when(plugin).playerIsOnIsland(player);
        doReturn(false).when(plugin).playerIsInSpawn(player);

        itemDropEvents.onDeathEvent(event);

        verify(event).setKeepInventory(true);
        verify(event, never()).getDrops();
    }

    @Test
    public void pickupIsBlockedWhenVisitorTakesForeignLoot() {
        World world = mock(World.class);
        Player player = mock(Player.class);
        doReturn(world).when(player).getWorld();
        doReturn("Notch").when(player).getName();

        // Loot tagged as belonging to someone else.
        String foreignTag = I18nUtil.trLegacy("Owner: <player>", unparsed("player", "Herobrine"));
        ItemMeta meta = mock(ItemMeta.class);
        doReturn(new ArrayList<>(List.of(foreignTag))).when(meta).getLore();
        ItemStack stack = mock(ItemStack.class);
        doReturn(meta).when(stack).getItemMeta();
        Item itemEntity = mock(Item.class);
        doReturn(stack).when(itemEntity).getItemStack();

        EntityPickupItemEvent event = mock(EntityPickupItemEvent.class);
        doReturn(player).when(event).getEntity();
        doReturn(false).when(event).isCancelled();
        doReturn(itemEntity).when(event).getItem();

        doReturn(true).when(worldManager).isSkyAssociatedWorld(world);
        doReturn(false).when(plugin).playerIsOnIsland(player);
        doReturn(false).when(plugin).playerIsInSpawn(player);

        itemDropEvents.onPickupEvent(event);

        verify(event).setCancelled(true);
        verify(plugin).notifyPlayer(eq(player), any());
    }

    @Test
    public void pickupAllowsOwnerToReclaimOwnLootAndClearsTag() {
        World world = mock(World.class);
        Player player = mock(Player.class);
        doReturn(world).when(player).getWorld();
        doReturn("Notch").when(player).getName();

        // Loot tagged as belonging to this player (round-trip from a previous drop).
        String ownerTag = I18nUtil.trLegacy("Owner: <player>", unparsed("player", "Notch"));
        ItemMeta meta = mock(ItemMeta.class);
        doReturn(new ArrayList<>(List.of(ownerTag))).when(meta).getLore();
        ItemStack stack = mock(ItemStack.class);
        doReturn(meta).when(stack).getItemMeta();
        Item itemEntity = mock(Item.class);
        doReturn(stack).when(itemEntity).getItemStack();

        EntityPickupItemEvent event = mock(EntityPickupItemEvent.class);
        doReturn(player).when(event).getEntity();
        doReturn(false).when(event).isCancelled();
        doReturn(itemEntity).when(event).getItem();

        doReturn(true).when(worldManager).isSkyAssociatedWorld(world);

        itemDropEvents.onPickupEvent(event);

        // Owner recognised: pickup is allowed and the ownership tag is stripped.
        verify(event, never()).setCancelled(true);
        verify(stack).setItemMeta(meta);
        verify(itemEntity).setItemStack(stack);

        ArgumentCaptor<List> loreCaptor = ArgumentCaptor.forClass(List.class);
        verify(meta).setLore(loreCaptor.capture());
        assertTrue(loreCaptor.getValue().isEmpty());
    }

    private RuntimeConfigs runtimeConfigs(boolean visitorItemDrops) {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("options.protection.visitors.item-drops", visitorItemDrops);
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        doReturn(runtimeConfig).when(runtimeConfigs).current();
        return runtimeConfigs;
    }
}
