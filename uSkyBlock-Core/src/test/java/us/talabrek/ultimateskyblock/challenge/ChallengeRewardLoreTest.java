package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class ChallengeRewardLoreTest {

    @BeforeAll
    public static void beforeAll() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
    }

    @Test
    public void wrappedRewardLinesKeepTheRewardColour() {
        assertThat(legacy(Challenge.rewardLore("a bucket of lava, some ice and one slime block")), contains(
            "§7Item reward: §ba bucket of lava, some",
            "§bice and one slime block"));
    }

    @Test
    public void wrappedRewardLinesKeepTheAdminsColour() {
        assertThat(legacy(Challenge.rewardLore("§ca bucket of lava, §esome ice and one slime block")), contains(
            "§7Item reward: §ca bucket of lava, §esome",
            "§eice and one slime block"));
    }

    private static List<String> legacy(List<Component> lore) {
        return lore.stream().map(LegacyComponentSerializer.legacySection()::serialize).toList();
    }
}
