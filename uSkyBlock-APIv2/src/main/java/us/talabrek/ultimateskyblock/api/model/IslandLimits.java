package us.talabrek.ultimateskyblock.api.model;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class IslandLimits extends Model {
    protected final Island island;
    protected ConcurrentSkipListMap<Material, Integer> blockLimits;
    protected ConcurrentSkipListMap<String, Integer> pluginLimits;

    public IslandLimits(Island island) {
        this.island = island;
    }

    public Island getIsland() {
        return island;
    }

    public @NotNull Map<Material, Integer> getBlockLimits() {
        return Map.copyOf(blockLimits);
    }

    public @Nullable Integer getBlockLimit(@NotNull Material key) {
        return blockLimits.get(key);
    }

    public void setBlockLimit(@NotNull Material key, int value) {
        blockLimits.put(key, value);
        setDirty(true);
    }

    public void setBlockLimits(@NotNull Map<Material, Integer> blockLimits) {
        this.blockLimits = new ConcurrentSkipListMap<>(blockLimits);
        setDirty(true);
    }

    public void removeBlockLimit(@NotNull Material key) {
        blockLimits.remove(key);
        setDirty(true);
    }

    public @NotNull Map<String, Integer> getPluginLimits() {
        return Map.copyOf(pluginLimits);
    }

    public @Nullable Integer getPluginLimit(@NotNull String key) {
        return pluginLimits.get(key);
    }

    public void setPluginLimit(@NotNull String key, int value) {
        pluginLimits.put(key, value);
        setDirty(true);
    }

    public void setPluginLimits(@NotNull Map<String, Integer> pluginLimits) {
        this.pluginLimits = new ConcurrentSkipListMap<>(pluginLimits);
        setDirty(true);
    }

    public void removePluginLimit(@NotNull String key) {
        pluginLimits.remove(key);
        setDirty(true);
    }
}
