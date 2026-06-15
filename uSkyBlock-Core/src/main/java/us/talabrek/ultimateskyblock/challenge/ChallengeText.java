package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.BlockMatcher;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ItemMatcher;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.TextSpec;

import java.util.Locale;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
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
        return itemMatcherName(spec.matcher());
    }

    private static @NotNull Component itemMatcherName(@NotNull ItemMatcher matcher) {
        return switch (matcher) {
            case ChallengeRequirements.ExactItem exact -> ItemStackUtil.getItemName(exact.item().create());
            case ChallengeRequirements.ItemTag itemTag -> tr("Any <tag>", unparsed("tag", humanizeTagKey(itemTag.key())));
            case ChallengeRequirements.AnyOfItems anyOf -> anyOfName(anyOf.matchers().stream().map(ChallengeText::itemMatcherName).toList());
        };
    }

    public static @NotNull Component blockName(@NotNull BlockMatcher matcher) {
        return switch (matcher) {
            case ChallengeRequirements.ExactBlock exact -> unparsedName(humanizeKey(exact.material().getKey().getKey()));
            case ChallengeRequirements.BlockTag blockTag -> tr("Any <tag>", unparsed("tag", humanizeTagKey(blockTag.key())));
            case ChallengeRequirements.AnyOfBlocks anyOf -> anyOfName(anyOf.matchers().stream().map(ChallengeText::blockName).toList());
        };
    }

    private static @NotNull Component anyOfName(@NotNull java.util.List<Component> names) {
        return tr("any of <options>", component("options", Component.join(JoinConfiguration.commas(true), names)));
    }

    private static @NotNull Component unparsedName(@NotNull String name) {
        return Component.text(name);
    }

    private static @NotNull String humanizeKey(@NotNull String key) {
        return key.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static @NotNull String humanizeTagKey(@NotNull String key) {
        int separator = key.indexOf(':');
        String path = separator >= 0 ? key.substring(separator + 1) : key;
        return path.replace('_', ' ');
    }
}
