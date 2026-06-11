package us.talabrek.ultimateskyblock.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.api.IslandRank;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

import java.util.Map;
import java.util.Set;

import static dk.lockfuglsang.minecraft.po.I18nUtil.fromLegacy;
import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;

/**
 * The single source of uSkyBlock placeholder values. Cheap keys resolve live through
 * the PlayerLogic/IslandLogic caches; creature counts come from main-thread-confined
 * {@link CreatureCountSnapshots}.
 */
@Singleton
public class IslandPlaceholderSource implements PlaceholderSource {

    private static final Component NOT_AVAILABLE = Component.text("N/A");
    private static final Component PENDING = Component.text("…");

    private static final Set<String> KEYS = Set.of(
        "version",
        "island_level", "island_level_int", "island_rank", "island_leader",
        "island_golems_max", "island_copper_golems_max", "island_monsters_max",
        "island_animals_max", "island_villagers_max", "island_partysize_max",
        "island_golems", "island_copper_golems", "island_monsters",
        "island_animals", "island_villagers", "island_partysize",
        "island_biome", "island_bans", "island_members", "island_trustees",
        "island_location", "island_location_x", "island_location_y", "island_location_z",
        "island_schematic");

    private final uSkyBlock plugin;
    private final PlayerLogic playerLogic;
    private final IslandLogic islandLogic;
    private final CreatureCountSnapshots creatureCounts;

    @Inject
    public IslandPlaceholderSource(
        @NotNull uSkyBlock plugin,
        @NotNull PlayerLogic playerLogic,
        @NotNull IslandLogic islandLogic,
        @NotNull CreatureCountSnapshots creatureCounts
    ) {
        this.plugin = plugin;
        this.playerLogic = playerLogic;
        this.islandLogic = islandLogic;
        this.creatureCounts = creatureCounts;
    }

    @Override
    public @NotNull Set<String> keys() {
        return KEYS;
    }

    @Override
    public @Nullable Component resolve(@NotNull OfflinePlayer player, @NotNull String key) {
        if (!KEYS.contains(key)) {
            return null;
        }
        if (key.equals("version")) {
            return Component.text(plugin.getDescription().getVersion());
        }
        PlayerInfo playerInfo = playerLogic.getPlayerInfo(player.getUniqueId());
        IslandInfo islandInfo = playerInfo != null ? islandLogic.getIslandInfo(playerInfo) : null;
        if (islandInfo == null) {
            return NOT_AVAILABLE;
        }
        return resolve(islandInfo, key);
    }

    private @NotNull Component resolve(@NotNull IslandInfo islandInfo, @NotNull String key) {
        return switch (key) {
            case "island_level" -> parseMini("<level:'##.#'>", number("level", islandInfo.getLevel()));
            case "island_level_int" -> parseMini("<level:'#'>", number("level", islandInfo.getLevel()));
            case "island_rank" -> rank(islandInfo);
            case "island_leader" -> fromLegacy(islandInfo.getLeader());
            case "island_golems_max" -> Component.text(islandInfo.getMaxGolems());
            case "island_copper_golems_max" -> Component.text(islandInfo.getMaxCopperGolems());
            case "island_monsters_max" -> Component.text(islandInfo.getMaxMonsters());
            case "island_animals_max" -> Component.text(islandInfo.getMaxAnimals());
            case "island_villagers_max" -> Component.text(islandInfo.getMaxVillagers());
            case "island_partysize_max" -> Component.text(islandInfo.getMaxPartySize());
            case "island_golems" -> creatureCount(islandInfo, LimitLogic.CreatureType.GOLEM);
            case "island_copper_golems" -> creatureCount(islandInfo, LimitLogic.CreatureType.COPPER_GOLEM);
            case "island_monsters" -> creatureCount(islandInfo, LimitLogic.CreatureType.MONSTER);
            case "island_animals" -> creatureCount(islandInfo, LimitLogic.CreatureType.ANIMAL);
            case "island_villagers" -> creatureCount(islandInfo, LimitLogic.CreatureType.VILLAGER);
            case "island_partysize" -> Component.text(islandInfo.getPartySize());
            case "island_biome" -> Component.text(islandInfo.getBiomeName());
            case "island_bans" -> Component.text(String.valueOf(islandInfo.getBans()));
            case "island_members" -> Component.text(String.valueOf(islandInfo.getMembers()));
            case "island_trustees" -> Component.text(String.valueOf(islandInfo.getTrustees()));
            case "island_location" -> Component.text(LocationUtil.asString(islandInfo.getIslandLocation()));
            case "island_location_x" -> parseMini("<x:'#'>", number("x", islandInfo.getIslandLocation().getBlockX()));
            case "island_location_y" -> parseMini("<y:'#'>", number("y", islandInfo.getIslandLocation().getBlockY()));
            case "island_location_z" -> parseMini("<z:'#'>", number("z", islandInfo.getIslandLocation().getBlockZ()));
            case "island_schematic" -> Component.text(islandInfo.getSchematicName());
            default -> throw new IllegalStateException("Key in KEYS but not handled: " + key);
        };
    }

    private @NotNull Component rank(@NotNull IslandInfo islandInfo) {
        IslandRank rank = islandLogic.getRank(islandInfo.getName());
        return rank != null
            ? parseMini("<rank:'#'>", number("rank", rank.getRank()))
            : NOT_AVAILABLE;
    }

    private @NotNull Component creatureCount(@NotNull IslandInfo islandInfo, @NotNull LimitLogic.CreatureType type) {
        Map<LimitLogic.CreatureType, Integer> counts = creatureCounts.counts(islandInfo);
        if (counts == null) {
            return PENDING;
        }
        return Component.text(counts.getOrDefault(type, 0));
    }
}
