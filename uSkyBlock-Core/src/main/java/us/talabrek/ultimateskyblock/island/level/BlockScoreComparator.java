package us.talabrek.ultimateskyblock.island.level;

import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import us.talabrek.ultimateskyblock.api.model.BlockScore;

import java.util.Comparator;

import static us.talabrek.ultimateskyblock.message.Msg.plainText;

/**
 * Comparator that sorts after score.
 */
public class BlockScoreComparator implements Comparator<BlockScore> {
    @Override
    public int compare(BlockScore o1, BlockScore o2) {
        int cmp = (int) Math.round(100 * (o2.getScore() - o1.getScore()));
        if (cmp == 0) {
            cmp = o2.getCount() - o1.getCount();
        }
        if (cmp == 0) {
            String name1 = plainText(ItemStackUtil.getBlockName(o1.getBlockData()));
            String name2 = plainText(ItemStackUtil.getBlockName(o2.getBlockData()));
            return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
        }
        return cmp;
    }
}
