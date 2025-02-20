package us.talabrek.ultimateskyblock.handler.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common PlaceholderAPI for internal placeholders.
 */
@Singleton
public class TextPlaceholder implements PlaceholderAPI {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(?<placeholder>usb_[^}]*)\\}");
    private final PlaceholderReplacer replacer;

    @Inject
    public TextPlaceholder(@NotNull PlaceholderReplacer replacer) {
        this.replacer = replacer;
    }

    @Override
    public @Nullable String replacePlaceholders(@Nullable Player player, @Nullable String message) {
        return replacePlaceholdersInternal(player, message);
    }

    private @Nullable String replacePlaceholdersInternal(@Nullable Player player, @Nullable String message) {
        if (message == null) {
            return null;
        }
        String result = message;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        if (matcher.find()) {
            int ix = 0;
            StringBuilder sb = new StringBuilder();
            do {
                sb.append(message, ix, matcher.start());
                String placeholderString = matcher.group("placeholder");
                if (placeholderString != null && replacer.getPlaceholders().contains(placeholderString)) {
                    String replacement = replacer.replace(null, player, placeholderString);
                    if (replacement != null) {
                        sb.append(replacement);
                    } else {
                        sb.append(message, matcher.start(), matcher.end());
                    }
                } else {
                    sb.append("{").append(placeholderString).append("}");
                }
                ix = matcher.end();
            } while (matcher.find());
            if (ix < message.length()) {
                sb.append(message.substring(ix));
            }
            result = sb.toString();
        }
        return result;
    }
}
