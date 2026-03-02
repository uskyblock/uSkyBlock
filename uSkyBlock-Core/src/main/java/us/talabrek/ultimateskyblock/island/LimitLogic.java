package us.talabrek.ultimateskyblock.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WaterMob;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.*;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.message.Placeholder.*;

@Singleton
public class LimitLogic {
    public enum CreatureType {UNKNOWN, ANIMAL, MONSTER, VILLAGER, GOLEM, COPPER_GOLEM}

    private final WorldManager worldManager;
    private final BlockLimitLogic blockLimitLogic;

    @Inject
    public LimitLogic(@NotNull WorldManager worldManager, @NotNull BlockLimitLogic blockLimitLogic) {
        this.worldManager = worldManager;
        this.blockLimitLogic = blockLimitLogic;
    }

    public Map<CreatureType, Integer> getCreatureCount(us.talabrek.ultimateskyblock.api.IslandInfo islandInfo) {
        Map<CreatureType, Integer> mapCount = new HashMap<>();
        for (CreatureType type : CreatureType.values()) {
            mapCount.put(type, 0);
        }
        Location islandLocation = islandInfo.getIslandLocation();
        ProtectedRegion islandRegionAt = WorldGuardHandler.getIslandRegionAt(islandLocation);
        if (islandRegionAt != null) {
            // Nether and Overworld regions are more or less equal (same x,z coords)
            List<LivingEntity> creatures = WorldGuardHandler.getCreaturesInRegion(worldManager.getWorld(),
                islandRegionAt);
            World nether = worldManager.getNetherWorld();
            if (nether != null) {
                creatures.addAll(WorldGuardHandler.getCreaturesInRegion(nether, islandRegionAt));
            }
            for (LivingEntity creature : creatures) {
                CreatureType key = getCreatureType(creature);
                if (!mapCount.containsKey(key)) {
                    mapCount.put(key, 0);
                }
                mapCount.put(key, mapCount.get(key) + 1);
            }
        }
        return mapCount;
    }

    public Map<CreatureType, Integer> getCreatureMax(us.talabrek.ultimateskyblock.api.IslandInfo islandInfo) {
        Map<CreatureType, Integer> max = new LinkedHashMap<>();
        for (CreatureType creatureType : CreatureType.values()) {
            max.put(creatureType, getMax(islandInfo, creatureType));
        }
        return max;
    }

    public CreatureType getCreatureType(LivingEntity creature) {
        if (creature instanceof Monster
            || creature instanceof WaterMob
            || creature instanceof Slime
            || creature instanceof Ghast) {
            return CreatureType.MONSTER;
        } else if (creature instanceof Animals) {
            return CreatureType.ANIMAL;
        } else if (creature instanceof Villager) {
            return CreatureType.VILLAGER;
        } else if (creature instanceof Golem) {
            if (creature.getType().equals(EntityType.COPPER_GOLEM)) {
                return CreatureType.COPPER_GOLEM;
            }
            return CreatureType.GOLEM;
        }
        return CreatureType.UNKNOWN;
    }

    public CreatureType getCreatureType(EntityType entityType) {
        if (Monster.class.isAssignableFrom(entityType.getEntityClass())
            || WaterMob.class.isAssignableFrom(entityType.getEntityClass())
            || Slime.class.isAssignableFrom(entityType.getEntityClass())
            || Ghast.class.isAssignableFrom(entityType.getEntityClass())
        ) {
            return CreatureType.MONSTER;
        } else if (Animals.class.isAssignableFrom(entityType.getEntityClass())) {
            return CreatureType.ANIMAL;
        } else if (Villager.class.isAssignableFrom(entityType.getEntityClass())) {
            return CreatureType.VILLAGER;
        } else if (Golem.class.isAssignableFrom(entityType.getEntityClass())) {
            if (entityType.equals(EntityType.COPPER_GOLEM)) {
                return CreatureType.COPPER_GOLEM;
            }
            return CreatureType.GOLEM;
        }
        return CreatureType.UNKNOWN;
    }

    public boolean canSpawn(EntityType entityType, us.talabrek.ultimateskyblock.api.IslandInfo islandInfo) {
        Map<CreatureType, Integer> creatureCount = getCreatureCount(islandInfo);
        CreatureType creatureType = getCreatureType(entityType);
        int max = getMax(islandInfo, creatureType);
        return !creatureCount.containsKey(creatureType) || creatureCount.get(creatureType) < max;
    }

    private int getMax(us.talabrek.ultimateskyblock.api.IslandInfo islandInfo, CreatureType creatureType) {
        return switch (creatureType) {
            case ANIMAL -> islandInfo.getMaxAnimals();
            case MONSTER -> islandInfo.getMaxMonsters();
            case VILLAGER -> islandInfo.getMaxVillagers();
            case COPPER_GOLEM -> islandInfo.getMaxCopperGolems();
            case GOLEM -> islandInfo.getMaxGolems();
            default -> Integer.MAX_VALUE;
        };
    }

    public String getSummary(us.talabrek.ultimateskyblock.api.IslandInfo islandInfo) {
        Map<LimitLogic.CreatureType, Integer> creatureMax = getCreatureMax(islandInfo);
        Map<LimitLogic.CreatureType, Integer> count = getCreatureCount(islandInfo);
        StringBuilder sb = new StringBuilder();
        for (LimitLogic.CreatureType key : creatureMax.keySet()) {
            if (key == CreatureType.UNKNOWN) {
                continue; // Skip
            }
            int cnt = count.getOrDefault(key, 0);
            int max = creatureMax.get(key);
            Component creatureCount = cnt >= max
                ? parseMini("<error><count>", unparsed("count", String.valueOf(cnt)))
                : Component.text(cnt);
            // I18N: A summary of entity group limits on an island
            sb.append(trLegacy("<entity-group>: <count> (max. <max>)",
                MUTED,
                component("entity-group", getCreatureTypeLabel(key)),
                component("count", creatureCount.applyFallbackStyle(SECONDARY)),
                unparsed("max", String.valueOf(max)))).append("\n");
        }
        Map<Material, Integer> blockLimits = blockLimitLogic.getLimits();
        for (Map.Entry<Material, Integer> entry : blockLimits.entrySet()) {
            int blockCount = blockLimitLogic.getCount(entry.getKey(), islandInfo.getIslandLocation());
            if (blockCount >= 0) {
                String current = blockCount >= entry.getValue()
                    ? miniToLegacy("<error><count>", number("count", blockCount))
                    : String.valueOf(blockCount);
                // I18N: A summary of block limits on an island
                sb.append(trLegacy("<block-type>: <count> (max. <max>)",
                    MUTED,
                    component("block-type", ItemStackUtil.getItemName(new ItemStack(entry.getKey()))),
                    legacy("count", current, SECONDARY),
                    number("max", entry.getValue()))).append("\n");
            } else {
                sb.append(trLegacy("<block-type>: <count> (max. <max>)",
                    MUTED,
                    component("block-type", ItemStackUtil.getItemName(new ItemStack(entry.getKey()))),
                    legacy("count", miniToLegacy("<error><unknown>", unparsed("unknown", "?")), SECONDARY),
                    number("max", entry.getValue()))).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private Component getCreatureTypeLabel(CreatureType creatureType) {
        return switch (creatureType) {
            // I18N: Creature category label shown in island mob-limit summary.
            case ANIMAL -> tr("Animals");
            // I18N: Creature category label shown in island mob-limit summary.
            case MONSTER -> tr("Monsters");
            // I18N: Creature category label shown in island mob-limit summary.
            case VILLAGER -> tr("Villagers");
            // I18N: Creature category label shown in island mob-limit summary.
            case GOLEM -> tr("Golems");
            // I18N: Creature category label shown in island mob-limit summary.
            case COPPER_GOLEM -> tr("Copper Golems");
            // I18N: Fallback creature category label shown in island mob-limit summary.
            case UNKNOWN -> tr("Unknown");
        };
    }
}
