package us.talabrek.ultimateskyblock.hook.economy;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VaultEconomyTest {
    private uSkyBlock plugin;
    private ServicesManager servicesManager;
    private PluginManager pluginManager;
    private Economy economy;
    private OfflinePlayer player;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();

        plugin = mock(uSkyBlock.class);
        Server server = mock(Server.class);
        servicesManager = mock(ServicesManager.class);
        pluginManager = mock(PluginManager.class);
        economy = mock(Economy.class);
        player = mock(OfflinePlayer.class);

        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getServicesManager()).thenReturn(servicesManager);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(economy.getName()).thenReturn("MockEconomy");
    }

    /**
     * Wraps a mocked Economy in a mocked RegisteredServiceProvider, matching what the
     * Bukkit ServicesManager hands back for a registered service.
     */
    private RegisteredServiceProvider<Economy> providerFor(Economy econ) {
        @SuppressWarnings("unchecked")
        RegisteredServiceProvider<Economy> rsp = mock(RegisteredServiceProvider.class);
        when(rsp.getProvider()).thenReturn(econ);
        return rsp;
    }

    @Test
    public void delegatesToProviderWhenPresent() {
        // doReturn(...) so providerFor's own mock/stub setup completes before this stubbing starts;
        // nesting it inside when(...).thenReturn(...) would corrupt Mockito state.
        doReturn(providerFor(economy)).when(servicesManager).getRegistration(Economy.class);
        VaultEconomy vault = new VaultEconomy(plugin);

        // registers itself as a listener during construction
        verify(pluginManager).registerEvents(vault, plugin);

        when(economy.getBalance(player)).thenReturn(123.45);
        when(economy.depositPlayer(player, 50.0))
            .thenReturn(new EconomyResponse(50.0, 100.0, ResponseType.SUCCESS, null));
        when(economy.withdrawPlayer(player, 30.0))
            .thenReturn(new EconomyResponse(30.0, 70.0, ResponseType.FAILURE, "no funds"));

        assertEquals(123.45, vault.getBalance(player), 1e-9);
        assertTrue(vault.depositPlayer(player, 50.0));
        assertFalse(vault.withdrawPlayer(player, 30.0));

        verify(economy).getBalance(player);
        verify(economy).depositPlayer(player, 50.0);
        verify(economy).withdrawPlayer(player, 30.0);
    }

    @Test
    public void fallsBackGracefullyWhenNoProvider() {
        when(servicesManager.getRegistration(Economy.class)).thenReturn(null);
        VaultEconomy vault = new VaultEconomy(plugin);

        // No provider resolved: fall back instead of NPEing on a null economy.
        assertEquals(0.0, vault.getBalance(player), 1e-9);
        assertFalse(vault.depositPlayer(player, 50.0));
        assertFalse(vault.withdrawPlayer(player, 30.0));
    }

    @Test
    public void resolvesProviderOnServiceRegisterEvent() {
        // Constructed with no economy available.
        when(servicesManager.getRegistration(Economy.class)).thenReturn(null);
        VaultEconomy vault = new VaultEconomy(plugin);
        assertEquals(0.0, vault.getBalance(player), 1e-9);

        // The service now becomes available, and the register event triggers re-resolution.
        // doReturn(...) so providerFor's own mock/stub setup completes before this stubbing starts;
        // nesting it inside when(...).thenReturn(...) would corrupt Mockito state.
        doReturn(providerFor(economy)).when(servicesManager).getRegistration(Economy.class);
        when(economy.getBalance(player)).thenReturn(77.0);
        vault.onEconomyRegister(new ServiceRegisterEvent(providerFor(economy)));

        assertEquals(77.0, vault.getBalance(player), 1e-9);
    }

    @Test
    public void clearsProviderOnServiceUnregisterEvent() {
        // Constructed with an economy provider present.
        // doReturn(...) so providerFor's own mock/stub setup completes before this stubbing starts;
        // nesting it inside when(...).thenReturn(...) would corrupt Mockito state.
        doReturn(providerFor(economy)).when(servicesManager).getRegistration(Economy.class);
        VaultEconomy vault = new VaultEconomy(plugin);
        when(economy.getBalance(player)).thenReturn(200.0);
        assertEquals(200.0, vault.getBalance(player), 1e-9);

        // Provider goes away: unregister event clears it and re-resolution finds nothing.
        when(servicesManager.getRegistration(Economy.class)).thenReturn(null);
        vault.onEconomyUnregister(new ServiceUnregisterEvent(providerFor(economy)));

        assertEquals(0.0, vault.getBalance(player), 1e-9);
    }
}
