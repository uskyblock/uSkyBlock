package us.talabrek.ultimateskyblock.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Composes the MiniMessage {@link TagResolver}s for placeholder substitution in
 * uSkyBlock's own texts. The internal {@code <usb:KEY>} tag is always available;
 * {@link PlaceholderIntegration}s contribute additional resolvers (e.g. {@code <papi:...>}).
 */
@Singleton
public class PlaceholderService {

    private final PlaceholderSource source;
    private final List<Function<Player, TagResolver>> contributions = new CopyOnWriteArrayList<>();

    @Inject
    public PlaceholderService(@NotNull PlaceholderSource source) {
        this.source = source;
    }

    /**
     * Registers an integration-provided resolver contribution. The function is invoked
     * per rendered message with the viewing player.
     */
    public void register(@NotNull Function<Player, TagResolver> contribution) {
        contributions.add(contribution);
    }

    /**
     * Returns the combined resolver for the given viewer: the internal {@code <usb:KEY>}
     * tag plus all registered integration contributions.
     */
    public @NotNull TagResolver resolvers(@NotNull Player viewer) {
        TagResolver.Builder builder = TagResolver.builder();
        builder.resolver(usbResolver(viewer));
        for (Function<Player, TagResolver> contribution : contributions) {
            builder.resolver(contribution.apply(viewer));
        }
        return builder.build();
    }

    private @NotNull TagResolver usbResolver(@NotNull Player viewer) {
        return TagResolver.resolver("usb", (args, ctx) -> {
            if (!args.hasNext()) {
                return Tag.selfClosingInserting(Component.text("<usb>"));
            }
            String key = args.pop().value();
            Component value;
            try {
                value = source.resolve(viewer, key);
            } catch (RuntimeException e) {
                value = null; // corrupt/unloadable data: fall through to the literal tag
            }
            if (value == null) {
                // Unknown key: render the tag literally so misconfiguration stays visible
                // without degrading the rest of the message.
                return Tag.selfClosingInserting(Component.text("<usb:" + key + ">"));
            }
            return Tag.selfClosingInserting(value);
        });
    }
}
