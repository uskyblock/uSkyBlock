package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.po.I18nUtil;
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
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.hook.economy.EconomyHook;
import us.talabrek.ultimateskyblock.player.Perk;
import us.talabrek.ultimateskyblock.player.PerkLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused coverage of {@link RewardApplier}'s private {@code grantCurrency(Player, int)}: the
 * enable-flag gate and the perk reward-bonus multiplier applied before depositing. The method is
 * private and invoked here via reflection to keep the test scoped to exactly that logic.
 */
public class RewardApplierCurrencyTest {

    private uSkyBlock plugin;
    private RuntimeConfigs runtimeConfigs;
    private HookManager hookManager;
    private PerkLogic perkLogic;
    private EconomyHook economyHook;
    private Perk perk;
    private Player player;
    private RewardApplier rewardApplier;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        // grantCurrency renders a reward message via tr(...) on the deposit path.
        I18nUtil.initialize(new File("."), Locale.ENGLISH);

        plugin = mock(uSkyBlock.class);
        runtimeConfigs = mock(RuntimeConfigs.class);
        hookManager = mock(HookManager.class);
        perkLogic = mock(PerkLogic.class);
        economyHook = mock(EconomyHook.class);
        perk = mock(Perk.class);
        player = mock(Player.class);

        when(player.getName()).thenReturn("Tester");
        when(economyHook.getCurrenyName()).thenReturn("$");

        rewardApplier = new RewardApplier(plugin, runtimeConfigs, hookManager, perkLogic);
    }

    @Test
    public void noDepositWhenEconomyRewardsDisabled() throws Exception {
        // Even with an economy hook present, the disabled flag must short-circuit before any deposit.
        setEconomyRewardsEnabled(false);
        when(hookManager.getEconomyHook()).thenReturn(Optional.of(economyHook));

        grantCurrency(100);

        verify(economyHook, never()).depositPlayer(any(), anyDouble());
    }

    @Test
    public void depositsFaceAmountWhenNoPerkBonus() throws Exception {
        setEconomyRewardsEnabled(true);
        stubPerkBonus(0.0);
        when(hookManager.getEconomyHook()).thenReturn(Optional.of(economyHook));

        grantCurrency(100);

        // rewBonus multiplier = 1 + 0.0, so the deposit equals the face amount.
        verify(economyHook).depositPlayer(player, 100.0);
    }

    @Test
    public void depositsAmountScaledByPerkBonus() throws Exception {
        setEconomyRewardsEnabled(true);
        stubPerkBonus(0.5);
        when(hookManager.getEconomyHook()).thenReturn(Optional.of(economyHook));

        grantCurrency(100);

        // amount * (1 + rewBonus) = 100 * 1.5 = 150.
        verify(economyHook).depositPlayer(player, 150.0);
    }

    @Test
    public void noDepositWhenNoEconomyHookAvailable() throws Exception {
        // Enabled, but no economy provider is hooked: the ifPresent branch is skipped, no NPE.
        setEconomyRewardsEnabled(true);
        stubPerkBonus(0.0);
        when(hookManager.getEconomyHook()).thenReturn(Optional.empty());

        grantCurrency(100);

        verify(economyHook, never()).depositPlayer(any(), anyDouble());
    }

    private void setEconomyRewardsEnabled(boolean enabled) {
        // RuntimeConfig is a record and cannot be mocked, so load a real one whose only relevant
        // input is the economy-rewards flag that grantCurrency gates on.
        YamlConfiguration config = baseConfig();
        config.set("options.challenges.enable-economy-rewards", enabled);
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger())
            .load(config);
        doReturn(runtimeConfig).when(runtimeConfigs).current();
    }

    /**
     * Minimal config that the RuntimeConfigFactory can fully load (mirrors InternalEventsTest).
     */
    private YamlConfiguration baseConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("language", "en");
        config.set("options.general.worldName", "skyworld");
        config.set("options.general.cooldownRestart", "1m");
        config.set("options.general.biomeChange", "1m");
        config.set("options.general.defaultBiome", "plains");
        config.set("options.general.defaultNetherBiome", "nether_wastes");
        config.set("options.advanced.confirmTimeout", "30s");
        config.set("options.advanced.playerdb.storage", "file");
        config.set("options.advanced.playerdb.nameCache", "maximumSize=100");
        config.set("options.advanced.playerdb.uuidCache", "maximumSize=100");
        config.set("options.advanced.playerCache", "maximumSize=100");
        config.set("options.advanced.islandCache", "maximumSize=100");
        config.set("options.advanced.island.saveEvery", 60);
        config.set("options.advanced.player.saveEvery", 60);
        config.set("options.party.invite-timeout", "1m");
        config.set("options.island.distance", 110);
        config.set("options.island.height", 120);
        config.set("options.island.topTenTimeout", "15m");
        config.set("options.island.islandTeleportDelay", "2s");
        config.set("options.island.chat-format", "default");
        config.set("options.party.chat-format", "default");
        config.set("nether.chunk-generator", "default");
        config.set("plugin-updates.branch", "LATEST");
        return config;
    }

    private void stubPerkBonus(double rewBonus) {
        when(perkLogic.getPerk(player)).thenReturn(perk);
        when(perk.getRewBonus()).thenReturn(rewBonus);
    }

    private void grantCurrency(int amount) throws Exception {
        Method method = RewardApplier.class.getDeclaredMethod("grantCurrency", Player.class, int.class);
        method.setAccessible(true);
        method.invoke(rewardApplier, player, amount);
    }
}
