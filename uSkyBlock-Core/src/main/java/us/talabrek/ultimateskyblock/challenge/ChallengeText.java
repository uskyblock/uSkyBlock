package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.TextSpec;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

/**
 * Rendering helpers for catalog display text (MiniMessage source).
 */
public final class ChallengeText {
    private ChallengeText() {
    }

    public static @NotNull Component displayName(@NotNull ChallengeDefinition challenge) {
        return render(challenge.display().name());
    }

    public static @NotNull Component displayName(@NotNull RankDefinition rank) {
        return render(rank.display().name());
    }

    public static @NotNull Component render(@NotNull TextSpec text) {
        return MiniMessage.miniMessage().deserialize(text.source());
    }

    public static @NotNull String plain(@NotNull TextSpec text) {
        return MiniMessage.miniMessage().stripTags(text.source());
    }

    public static @NotNull String plainName(@NotNull ChallengeDefinition challenge) {
        return plain(challenge.display().name());
    }

    public static @NotNull Component itemName(@NotNull ChallengeRequirements.ItemRequirementSpec spec) {
        return switch (spec.matcher()) {
            case ChallengeRequirements.ExactItem exact -> ItemStackUtil.getItemName(exact.item().create());
            case ChallengeRequirements.ItemTag itemTag -> tr("Any <tag>",
                unparsed("tag", humanizeTagKey(itemTag.key())));
        };
    }

    private static @NotNull String humanizeTagKey(@NotNull String key) {
        int separator = key.indexOf(':');
        String path = separator >= 0 ? key.substring(separator + 1) : key;
        return path.replace('_', ' ');
    }
}
