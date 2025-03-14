package us.talabrek.ultimateskyblock.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.island.IslandGenerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for calculating player specific perks based on permissions.
 */
@Singleton
public class PerkLogic {
    private final Perk defaultPerk;
    private final Map<String, Perk> donorPerks;
    private final Map<String, IslandPerk> islandPerks;

    @Inject
    public PerkLogic(
        @NotNull IslandGenerator islandGenerator,
        @NotNull PluginConfig config
    ) {
        defaultPerk = new Perk(Collections.emptyList(), Settings.general_maxPartySize,
            config.getYamlConfig().getInt("options.island.spawn-limits.animals", 30),
            config.getYamlConfig().getInt("options.island.spawn-limits.monsters", 50),
            config.getYamlConfig().getInt("options.island.spawn-limits.villagers", 16),
            config.getYamlConfig().getInt("options.island.spawn-limits.golems", 4),
            0,
            0,
            null, null);
        donorPerks = new ConcurrentHashMap<>();
        addDonorPerks(null, config.getYamlConfig().getConfigurationSection("donor-perks"));
        addExtraPermissionPerks(config.getYamlConfig().getConfigurationSection("options.island.extraPermissions"));
        addPartyPermissionPerks(null, config.getYamlConfig().getConfigurationSection("options.party.maxPartyPermissions"));
        addHungerPerms();
        addDonorRewardPerks();
        List<String> schemeNames = islandGenerator.getSchemeNames();
        addSchemePerks(schemeNames);

        islandPerks = new ConcurrentHashMap<>();

        ConfigurationSection islandSchemes = config.getYamlConfig().getConfigurationSection("island-schemes");
        if (islandSchemes != null) {
            for (String schemeName : islandSchemes.getKeys(false)) {
                ConfigurationSection configSection = islandSchemes.getConfigurationSection(schemeName);
                String perm = configSection.getString("permission", "usb.schematic." + schemeName);
                Perk perk = new PerkBuilder()
                    .schematics(schemeName)
                    .maxPartySize(configSection.getInt("maxPartySize", 0))
                    .animals(configSection.getInt("animals", 0))
                    .monsters(configSection.getInt("monsters", 0))
                    .villagers(configSection.getInt("villagers", 0))
                    .golems(configSection.getInt("golems", 0))
                    .rewBonus(configSection.getInt("rewardBonus", 0))
                    .hungerReduction(configSection.getInt("hungerReduction", 0))
                    .extraItems(ItemStackUtil.createItemList(configSection.getStringList("extraItems")))
                    .build();
                ItemStack itemStack = ItemStackUtil.createItemStack(
                    configSection.getString("displayItem", Material.OAK_SAPLING.toString()),
                    schemeName,
                    configSection.getString("description", null)
                );
                islandPerks.put(schemeName, new IslandPerk(schemeName, perm, itemStack, perk,
                    configSection.getDouble("scoreMultiply", 1d), configSection.getDouble("scoreOffset", 0d)));
            }
        }
        for (String schemeName : schemeNames) {
            Perk perk = new PerkBuilder(defaultPerk).schematics(schemeName).build();
            if (!islandPerks.containsKey(schemeName)) {
                islandPerks.put(schemeName, new IslandPerk(schemeName, "usb.schematic." + schemeName,
                    ItemStackUtil.createItemStack(Material.OAK_SAPLING.toString(), schemeName, null), perk,
                    1d, 0d));
            }
        }
    }

    public Perk getDefaultPerk() {
        return defaultPerk;
    }

    public Perk getPerk(Player player) {
        return createPerk(player);
    }

    public Set<String> getSchemes(Player player) {
        Set<String> schemes = new LinkedHashSet<>();
        for (IslandPerk islandPerk : islandPerks.values()) {
            if (player.hasPermission(islandPerk.getPermission())) {
                schemes.add(islandPerk.getSchemeName());
            }
        }
        return schemes;
    }

    public IslandPerk getIslandPerk(String schemeName) {
        if (islandPerks.containsKey(schemeName)) {
            return islandPerks.get(schemeName);
        }
        return new IslandPerk(schemeName, "usb.schematic." + schemeName,
            ItemStackUtil.createItemStack(Material.GRASS_BLOCK.toString(), schemeName, null), defaultPerk);
    }

    private Perk createPerk(Player player) {
        PerkBuilder builder = new PerkBuilder(defaultPerk);
        for (String perm : donorPerks.keySet()) {
            if (player.hasPermission(perm)) {
                builder.combine(donorPerks.get(perm));
            }
        }
        return builder.build();
    }

    private void addDonorPerks(String perm, ConfigurationSection config) {
        if (config == null) {
            return;
        }
        for (String key : config.getKeys(false)) {
            if (config.isConfigurationSection(key)) {
                addDonorPerks((perm != null ? perm + "." : "") + key, config.getConfigurationSection(key));
            } else {
                // Read leaf
                donorPerks.put(perm, new Perk(
                    ItemStackUtil.createItemList(config.getStringList("extraItems")),
                    config.getInt("maxPartySize", defaultPerk.getMaxPartySize()),
                    config.getInt("animals", defaultPerk.getAnimals()),
                    config.getInt("monsters", defaultPerk.getMonsters()),
                    config.getInt("villagers", defaultPerk.getVillagers()),
                    config.getInt("golems", defaultPerk.getGolems()),
                    config.getDouble("rewardBonus", defaultPerk.getRewBonus()),
                    config.getDouble("hungerReduction", defaultPerk.getHungerReduction()),
                    config.getStringList("schematics"), null));
            }
        }
    }

    private void addExtraPermissionPerks(ConfigurationSection config) {
        if (config == null) {
            return;
        }
        for (String key : config.getKeys(false)) {
            List<ItemStack> items = ItemStackUtil.createItemList(config.getStringList(key));
            if (items != null && !items.isEmpty()) {
                String perm = "usb." + key;
                donorPerks.put(perm, new PerkBuilder(donorPerks.get(perm))
                    .extraItems(items)
                    .build());
            }
        }
    }

    private void addPartyPermissionPerks(String perm, ConfigurationSection config) {
        int[] values = {5, 6, 7, 8};
        String[] perms = {"usb.extra.partysize1", "usb.extra.partysize2", "usb.extra.partysize3", "usb.extra.partysize"};
        for (int i = 0; i < values.length; i++) {
            donorPerks.put(perms[i],
                new PerkBuilder(donorPerks.get(perms[i]))
                    .maxPartySize(values[i])
                    .build());
        }

        if (config == null) {
            return;
        }
        for (String key : config.getKeys(false)) {
            if (config.isConfigurationSection(key)) {
                addPartyPermissionPerks((perm != null ? perm + "." : "") + key, config.getConfigurationSection(key));
            } else if (config.isInt(key)) {
                // Read leaf
                donorPerks.put(perm, new PerkBuilder(donorPerks.get(perm))
                    .maxPartySize(config.getInt(key, 0))
                    .build());
            }
        }
    }

    private void addHungerPerms() {
        double[] values = {1, 0.75, 0.50, 0.25};
        String[] perms = {"usb.extra.hunger4", "usb.extra.hunger3", "usb.extra.hunger2", "usb.extra.hunger"};
        for (int i = 0; i < values.length; i++) {
            donorPerks.put(perms[i],
                new PerkBuilder(donorPerks.get(perms[i]))
                    .hungerReduction(values[i])
                    .build());
        }
    }

    private void addDonorRewardPerks() {
        // Note: This is NOT the same as before, but it's trying to be as close as possible.
        double[] values = {0.05, 0.10, 0.15, 0.20, 0.30, 0.50};
        String[] perms = {"group.memberplus", "usb.donor.all", "usb.donor.25", "usb.donor.50", "usb.donor.75", "usb.donor.100"};
        for (int i = 0; i < values.length; i++) {
            donorPerks.put(perms[i],
                new PerkBuilder(donorPerks.get(perms[i]))
                    .rewBonus(values[i])
                    .build());
        }
    }

    private void addSchemePerks(List<String> schemeNames) {
        if (schemeNames == null) {
            return;
        }
        for (String scheme : schemeNames) {
            String perm = "usb.schematic." + scheme;
            donorPerks.put(perm, new PerkBuilder(donorPerks.get(perm))
                .schematics(scheme)
                .build());
        }
    }

    public Map<String, Perk> getPerkMap() {
        return Collections.unmodifiableMap(donorPerks);
    }

    public static class PerkBuilder {
        private Perk perk;

        public PerkBuilder() {
            perk = new Perk(null, 0, 0, 0, 0, 0, 0, 0, null, null);
        }

        public PerkBuilder(Perk basePerk) {
            perk = basePerk != null ? basePerk : new Perk(null, 0, 0, 0, 0, 0, 0, 0, null, null);
        }

        public PerkBuilder extraItems(List<ItemStack> items) {
            perk = perk.combine(new Perk(items, 0, 0, 0, 0, 0, 0, 0, null, null));
            return this;
        }

        public PerkBuilder maxPartySize(int max) {
            perk = perk.combine(new Perk(null, max, 0, 0, 0, 0, 0, 0, null, null));
            return this;
        }

        public PerkBuilder animals(int animals) {
            perk = perk.combine(new Perk(null, 0, animals, 0, 0, 0, 0, 0, null, null));
            return this;
        }

        public PerkBuilder monsters(int monsters) {
            perk = perk.combine(new Perk(null, 0, 0, monsters, 0, 0, 0, 0, null, null));
            return this;
        }

        public PerkBuilder villagers(int villagers) {
            perk = perk.combine(new Perk(null, 0, 0, 0, villagers, 0, 0, 0, null, null));
            return this;
        }

        public PerkBuilder golems(int golems) {
            perk = perk.combine(new Perk(null, 0, 0, 0, 0, golems, 0, 0, null, null));
            return this;
        }

        public PerkBuilder rewBonus(double bonus) {
            perk = perk.combine(new Perk(null, 0, 0, 0, 0, 0, bonus, 0, null, null));
            return this;
        }

        public PerkBuilder hungerReduction(double reduction) {
            perk = perk.combine(new Perk(null, 0, 0, 0, 0, 0, 0, reduction, null, null));
            return this;
        }

        public PerkBuilder schematics(String... schemes) {
            perk = perk.combine(new Perk(null, 0, 0, 0, 0, 0, 0, 0, Arrays.asList(schemes), null));
            return this;
        }

        public PerkBuilder combine(Perk other) {
            if (other != null) {
                perk = perk.combine(other);
            }
            return this;
        }

        public Perk build() {
            return perk;
        }
    }
}
