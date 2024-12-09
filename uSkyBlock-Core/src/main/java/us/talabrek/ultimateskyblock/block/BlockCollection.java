package us.talabrek.ultimateskyblock.block;

import dk.lockfuglsang.minecraft.util.BlockRequirement;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

public class BlockCollection {
    Map<Material, Integer> blockCount;

    public BlockCollection() {
        this.blockCount = new HashMap<>();
    }

    public void add(Block block) {
        int currentValue = blockCount.getOrDefault(block.getType(), 0);
        blockCount.put(block.getType(), currentValue + 1);
    }

    /**
     * Returns <code>null</code> if all the items are in the BlockCollection, a String describing the missing items if it's not
     */
    public String diff(List<BlockRequirement> requirements) {
        StringBuilder sb = new StringBuilder();
        for (BlockRequirement requirement : requirements) {
            int diff = requirement.amount() - count(requirement.type().getMaterial());
            if (diff > 0) {
                sb.append(tr(" \u00a7f{0}x \u00a77{1}", diff, ItemStackUtil.getBlockName(requirement.type())));
            }
        }
        if (sb.toString().trim().isEmpty()) {
            return null;
        }
        return tr("\u00a7eStill the following blocks short: {0}", sb.toString());
    }

    private int count(Material type) {
        return blockCount.getOrDefault(type, 0);
    }
}
