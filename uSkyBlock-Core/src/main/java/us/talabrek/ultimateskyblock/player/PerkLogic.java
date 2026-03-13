package us.talabrek.ultimateskyblock.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.ItemStackAmountSpec;
import us.talabrek.ultimateskyblock.handler.SchematicHandler;

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
        @NotNull SchematicHandler schematicHandler,
        @NotNull RuntimeConfigs runtimeConfigs
    ) {
        RuntimeConfig runtimeConfig = runtimeConfigs.current();
        defaultPerk = new Perk(Collections.emptyList(), runtimeConfig.general().maxPartySize(),
            runtimeConfig.island().spawnLimits().animals(),
            runtimeConfig.island().spawnLimits().monsters(),
            runtimeConfig.island().spawnLimits().villagers(),
            runtimeConfig.island().spawnLimits().golems(),
            runtimeConfig.island().spawnLimits().copperGolems(),
            0,
            0,
            null, null);
        donorPerks = new ConcurrentHashMap<>();
        addDonorPerks(runtimeConfig.donorPerks());
        addExtraPermissionPerks(runtimeConfig.island().extraPermissionItems());
        addPartyPermissionPerks(runtimeConfig.party().maxPartyPermissions());
        addHungerPerms();
        addDonorRewardPerks();
        List<String> schemeNames = schematicHandler.getSchemeNames();
        addSchemePerks(schemeNames);

        islandPerks = new ConcurrentHashMap<>();

        for (var entry : runtimeConfig.islandSchemes().entrySet()) {
            String schemeName = entry.getKey();
            RuntimeConfig.IslandScheme scheme = entry.getValue();
            String perm = scheme.permission();
                Perk perk = new PerkBuilder()
                    .schematics(schemeName)
                    .maxPartySize(scheme.maxPartySize())
                    .animals(scheme.animals())
                    .monsters(scheme.monsters())
                    .villagers(scheme.villagers())
                    .golems(scheme.golems())
                    .copperGolems(scheme.copperGolems())
                    .extraItems(createItems(scheme.extraItems()))
                    .build();
                ItemStack itemStack = scheme.displayItem().create(schemeName, scheme.description());
                islandPerks.put(schemeName, new IslandPerk(schemeName, perm, itemStack, perk,
                    scheme.scoreMultiply(), scheme.scoreOffset()));
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

    private void addDonorPerks(Map<String, RuntimeConfig.PerkSpec> configuredPerks) {
        for (var entry : configuredPerks.entrySet()) {
            RuntimeConfig.PerkSpec perk = entry.getValue();
            donorPerks.put(entry.getKey(), new Perk(
                createItems(perk.extraItems()),
                perk.maxPartySize() > 0 ? perk.maxPartySize() : defaultPerk.getMaxPartySize(),
                perk.animals() > 0 ? perk.animals() : defaultPerk.getAnimals(),
                perk.monsters() > 0 ? perk.monsters() : defaultPerk.getMonsters(),
                perk.villagers() > 0 ? perk.villagers() : defaultPerk.getVillagers(),
                perk.golems() > 0 ? perk.golems() : defaultPerk.getGolems(),
                perk.copperGolems() > 0 ? perk.copperGolems() : defaultPerk.getCopperGolems(),
                perk.rewardBonus(),
                perk.hungerReduction(),
                perk.schematics(), null));
        }
    }

    private void addExtraPermissionPerks(Map<String, List<ItemStackAmountSpec>> extraPermissionItems) {
        for (var entry : extraPermissionItems.entrySet()) {
            String key = entry.getKey();
            List<ItemStack> items = createItems(entry.getValue());
            if (items != null && !items.isEmpty()) {
                String perm = "usb." + key;
                donorPerks.put(perm, new PerkBuilder(donorPerks.get(perm))
                    .extraItems(items)
                    .build());
            }
        }
    }

    private void addPartyPermissionPerks(Map<String, Integer> partyPermissionOverrides) {
        int[] values = {5, 6, 7, 8};
        String[] perms = {"usb.extra.partysize1", "usb.extra.partysize2", "usb.extra.partysize3", "usb.extra.partysize"};
        for (int i = 0; i < values.length; i++) {
            donorPerks.put(perms[i],
                new PerkBuilder(donorPerks.get(perms[i]))
                    .maxPartySize(values[i])
                    .build());
        }

        for (var entry : partyPermissionOverrides.entrySet()) {
            donorPerks.put(entry.getKey(), new PerkBuilder(donorPerks.get(entry.getKey()))
                    .maxPartySize(entry.getValue())
                    .build());
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

    private static List<ItemStack> createItems(List<ItemStackAmountSpec> specs) {
        return specs.stream().flatMap(spec -> spec.stacks().stream()).toList();
    }

    public static class PerkBuilder {
        private Perk perk;

        public PerkBuilder() {
            perk = new Perk(null, 0, 0, 0, 0, 0, 0, 0, 0, null, null);
        }

        public PerkBuilder(Perk basePerk) {
            perk = basePerk != null ? basePerk : new Perk(null, 0, 0, 0, 0, 0, 0, 0, 0, null, null);
        }

        public PerkBuilder extraItems(List<ItemStack> items) {
            perk = perk.combine(new Perk(items, 0, 0, 0, 0, 0, 0, 0, 0, null, null));
            return this;
        }

        public PerkBuilder maxPartySize(int max) {
            perk = perk.combine(new Perk(null, max, 0, 0, 0, 0, 0, 0, 0, null, null));
            return this;
        }

        public PerkBuilder animals(int animals) {
            perk = perk.combine(new Perk(null, 0, animals, 0, 0, 0, 0, 0, 0, null, null));
            return this;
        }

        public PerkBuilder monsters(int monsters) {
            perk = perk.combine(new Perk(null, 0, 0, monsters, 0, 0, 0, 0, 0, null, null));
            return this;
        }

        public PerkBuilder villagers(int villagers) {
            perk = perk.combine(new Perk(null, 0, 0, 0, villagers, 0, 0, 0, 0, null, null));
            return this;
        }

        public PerkBuilder golems(int golems) {
            perk = perk.combine(new Perk(null, 0, 0, 0, 0, golems, 0, 0, 0, null, null));
            return this;
        }

        public PerkBuilder copperGolems(int copperGolems) {
            perk = perk.combine(new Perk(null, 0, 0, 0, 0, 0, copperGolems, 0, 0, null, null));
            return this;
        }

        public PerkBuilder rewBonus(double bonus) {
            perk = perk.combine(new Perk(null, 0, 0, 0, 0, 0, 0, bonus, 0, null, null));
            return this;
        }

        public PerkBuilder hungerReduction(double reduction) {
            perk = perk.combine(new Perk(null, 0, 0, 0, 0, 0, 0, 0, reduction, null, null));
            return this;
        }

        public PerkBuilder schematics(String... schemes) {
            perk = perk.combine(new Perk(null, 0, 0, 0, 0, 0, 0, 0, 0, Arrays.asList(schemes), null));
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
