package us.talabrek.ultimateskyblock.handler.placeholder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.IslandRank;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static dk.lockfuglsang.minecraft.po.I18nUtil.pre;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * The actual replacer for placeholders
 */
@Singleton
public class PlaceholderReplacerImpl implements PlaceholderAPI.PlaceholderReplacer {
    private static final Collection<String> PLACEHOLDERS = Set.of(
        "usb_version",
        "usb_island_level",
        "usb_island_level_int",
        "usb_island_rank",
        "usb_island_leader",
        "usb_island_golems_max",
        "usb_island_monsters_max",
        "usb_island_animals_max",
        "usb_island_villagers_max",
        "usb_island_partysize_max",
        "usb_island_golems",
        "usb_island_monsters",
        "usb_island_animals",
        "usb_island_villagers",
        "usb_island_partysize",
        "usb_island_biome",
        "usb_island_bans",
        "usb_island_members",
        "usb_island_trustees",
        "usb_island_location",
        "usb_island_location_x",
        "usb_island_location_y",
        "usb_island_location_z",
        "usb_island_schematic"
    );
    private final uSkyBlock plugin;
    private final PlayerLogic playerLogic;
    private final IslandLogic islandLogic;
    private final LimitLogic limitLogic;
    private final LoadingCache<CacheEntry, String> cache;

    @Inject
    public PlaceholderReplacerImpl(
        @NotNull uSkyBlock plugin,
        @NotNull PlayerLogic playerLogic,
        @NotNull IslandLogic islandLogic,
        @NotNull LimitLogic limitLogic
    ) {
        this.plugin = plugin;
        this.playerLogic = playerLogic;
        this.islandLogic = islandLogic;
        this.limitLogic = limitLogic;

        this.cache = CacheBuilder
            .from(plugin.getConfig().getString("options.advanced.placeholderCache",
                "maximumSize=200,expireAfterWrite=20s"))
            .build(new CacheLoader<>() {
                @Override
                public @NotNull String load(@NotNull CacheEntry cacheEntry) throws Exception {
                    try {
                        return lookup(cacheEntry);
                    } catch (RuntimeException e) {
                        throw new ExecutionException(e.getMessage(), e);
                    }
                }
            });
    }

    private String lookup(CacheEntry entry) {
        String placeholder = entry.placeholder();
        if (placeholder.startsWith("usb_island")) {
            PlayerInfo playerInfo = playerLogic.getPlayerInfo(entry.uuid());
            IslandInfo islandInfo = islandLogic.getIslandInfo(playerInfo);
            if (playerInfo == null || islandInfo == null) {
                return tr("N/A");
            }
            return lookup(islandInfo, placeholder);
        } else if (placeholder.startsWith("usb_")) {
            return lookup(placeholder);
        }
        throw new IllegalArgumentException("Unsupported placeholder " + placeholder);
    }

    private String lookup(String placeholder) {
        if (placeholder.equals("usb_version")) {
            return plugin.getDescription().getVersion();
        }
        throw new IllegalArgumentException("Unsupported placeholder " + placeholder);
    }

    private String lookup(IslandInfo islandInfo, String placeholder) {
        return switch (placeholder) {
            case "usb_island_level" -> pre("{0,number,##.#}", islandInfo.getLevel());
            case "usb_island_level_int" -> pre("{0,number,#}", islandInfo.getLevel());
            case "usb_island_rank" -> getRank(islandInfo);
            case "usb_island_leader" -> islandInfo.getLeader();
            case "usb_island_golems_max" -> "" + islandInfo.getMaxGolems();
            case "usb_island_monsters_max" -> "" + islandInfo.getMaxMonsters();
            case "usb_island_animals_max" -> "" + islandInfo.getMaxAnimals();
            case "usb_island_villagers_max" -> "" + islandInfo.getMaxVillagers();
            case "usb_island_partysize_max" -> "" + islandInfo.getMaxPartySize();
            case "usb_island_golems" -> "" + limitLogic.getCreatureCount(islandInfo).get(LimitLogic.CreatureType.GOLEM);
            case "usb_island_monsters" ->
                "" + limitLogic.getCreatureCount(islandInfo).get(LimitLogic.CreatureType.MONSTER);
            case "usb_island_animals" ->
                "" + limitLogic.getCreatureCount(islandInfo).get(LimitLogic.CreatureType.ANIMAL);
            case "usb_island_villagers" ->
                "" + limitLogic.getCreatureCount(islandInfo).get(LimitLogic.CreatureType.VILLAGER);
            case "usb_island_partysize" -> "" + islandInfo.getPartySize();
            case "usb_island_biome" -> islandInfo.getBiomeName();
            case "usb_island_bans" -> "" + islandInfo.getBans();
            case "usb_island_members" -> "" + islandInfo.getMembers();
            case "usb_island_trustees" -> "" + islandInfo.getTrustees();
            case "usb_island_location" -> LocationUtil.asString(islandInfo.getIslandLocation());
            case "usb_island_location_x" -> pre("{0,number,#}", islandInfo.getIslandLocation().getBlockX());
            case "usb_island_location_y" -> pre("{0,number,#}", islandInfo.getIslandLocation().getBlockY());
            case "usb_island_location_z" -> pre("{0,number,#}", islandInfo.getIslandLocation().getBlockZ());
            case "usb_island_schematic" -> islandInfo.getSchematicName();
            default -> throw new IllegalArgumentException("Unsupported placeholder " + placeholder);
        };
    }

    private String getRank(IslandInfo islandInfo) {
        IslandRank rank = islandLogic.getRank(islandInfo.getName());
        if (rank != null) {
            return pre("{0,number,#}", rank.getRank());
        } else {
            return tr("N/A");
        }
    }

    @Override
    public @NotNull Collection<String> getPlaceholders() {
        return PLACEHOLDERS;
    }

    @Override
    public @Nullable String replace(@Nullable OfflinePlayer offlinePlayer, @Nullable Player player, @Nullable String placeholder) {
        if (placeholder == null || !placeholder.startsWith("usb_")) {
            return null;
        }
        UUID uuid = player != null ? player.getUniqueId() : null;
        if (uuid == null && offlinePlayer != null) {
            uuid = offlinePlayer.getUniqueId();
        }
        if (uuid == null) {
            return null;
        }
        CacheEntry cacheKey = new CacheEntry(uuid, placeholder);
        try {
            return cache.get(cacheKey);
        } catch (ExecutionException e) {
            return null;
        }
    }

    private record CacheEntry(UUID uuid, String placeholder) {
    }
}
