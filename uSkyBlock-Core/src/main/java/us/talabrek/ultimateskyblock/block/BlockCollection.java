package us.talabrek.ultimateskyblock.block;

import dk.lockfuglsang.minecraft.util.BlockRequirement;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.miniToLegacy;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.MUTED;

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
                sb.append(miniToLegacy(" <secondary><count>x <muted><block>",
                    unparsed("count", String.valueOf(diff)),
                    legacyArg("block", ItemStackUtil.getBlockName(requirement.type()))));
            }
        }
        if (sb.toString().trim().isEmpty()) {
            return null;
        }
        return trLegacy("Still missing the following blocks: <blocks>", MUTED, legacyArg("blocks", sb.toString()));
    }

    private int count(Material type) {
        return blockCount.getOrDefault(type, 0);
    }
}
