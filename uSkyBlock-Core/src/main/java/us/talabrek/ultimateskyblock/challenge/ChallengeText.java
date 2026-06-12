package us.talabrek.ultimateskyblock.challenge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.TextSpec;

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
}
