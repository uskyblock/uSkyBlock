package us.talabrek.ultimateskyblock.hook.economy;

import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.math.BigDecimal;
import java.util.Optional;

public class VaultEconomy extends EconomyHook implements Listener {
    private Economy economy;

    public VaultEconomy(@NotNull uSkyBlock plugin) {
        super(plugin, "Vault");
        setupEconomy().ifPresent(vaultPlugin -> this.economy = vaultPlugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Optional<Economy> setupEconomy() {
        RegisteredServiceProvider<Economy> rsp =
            plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            plugin.getLogger().info("Using " + rsp.getProvider().getName() + " as economy provider.");
            return Optional.of(economy);
        }

        return Optional.empty();
    }

    @Override
    public @NotNull String getCurrenyName() {
        if (economy != null) {
            return economy.defaultCurrencyNamePlural(plugin.getName());
        }
        return super.getCurrenyName();
    }

    @Override
    public double getBalance(@NotNull OfflinePlayer player) {
        if (economy != null) {
            return economy.balance(plugin.getName(), player.getUniqueId()).doubleValue();
        }
        return 0;
    }

    @Override
    public @Nullable String depositPlayer(@NotNull OfflinePlayer player, double amount) {
        if (economy != null) {
            EconomyResponse response = economy.deposit(plugin.getName(), player.getUniqueId(), BigDecimal.valueOf(amount));
            if (response.transactionSuccess()) return null; else return response.errorMessage;
        }
        return "Economy is null";
    }

    @Override
    public @Nullable String withdrawPlayer(@NotNull OfflinePlayer player, double amount) {
        if (economy != null) {
            EconomyResponse response = economy.withdraw(plugin.getName(), player.getUniqueId(), BigDecimal.valueOf(amount));
            if (response.transactionSuccess()) return null; else return response.errorMessage;
        }
        return "Economy is null";
    }

    @EventHandler
    public void onEconomyRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getProvider() instanceof Economy) {
            setupEconomy().ifPresent(vaultPlugin -> this.economy = vaultPlugin);
        }
    }

    @EventHandler
    public void onEconomyUnregister(ServiceUnregisterEvent event) {
        if (event.getProvider().getProvider() instanceof Economy) {
            this.economy = null;
            setupEconomy().ifPresent(vaultPlugin -> this.economy = vaultPlugin);
        }
    }
}
