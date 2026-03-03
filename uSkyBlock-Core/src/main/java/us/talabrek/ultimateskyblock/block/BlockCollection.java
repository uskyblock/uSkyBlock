package us.talabrek.ultimateskyblock.block;

import dk.lockfuglsang.minecraft.util.BlockRequirement;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;

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
     * Returns <code>null</code> if all the blocks are present, a Component describing missing blocks otherwise.
     */
    public @Nullable Component diff(List<BlockRequirement> requirements) {
        Component missingBlocks = Component.empty();
        boolean hasMissing = false;
        for (BlockRequirement requirement : requirements) {
            int diff = requirement.amount() - count(requirement.type().getMaterial());
            if (diff > 0) {
                hasMissing = true;
                missingBlocks = missingBlocks.append(parseMini(" <secondary><count>x <muted><block>",
                    number("count", diff),
                    component("block", ItemStackUtil.getBlockName(requirement.type()))));
            }
        }
        if (!hasMissing) {
            return null;
        }
        return tr("Still missing the following blocks: <blocks>", MUTED, component("blocks", missingBlocks));
    }

    private int count(Material type) {
        return blockCount.getOrDefault(type, 0);
    }
}
