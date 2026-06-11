package us.talabrek.ultimateskyblock.papi;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.placeholder.PlaceholderIntegration;
import us.talabrek.ultimateskyblock.placeholder.PlaceholderService;
import us.talabrek.ultimateskyblock.placeholder.PlaceholderSource;
import us.talabrek.ultimateskyblock.uSkyBlock;

/**
 * PlaceholderAPI integration: registers the bundled {@code uskyblock} expansion
 * (offering values to other plugins) and contributes the {@code <papi:PLACEHOLDER>}
 * tag (rendering other plugins' placeholders in uSkyBlock texts). PAPI output is
 * deserialized as legacy section text — never MiniMessage-parsed (third-party,
 * partially player-influenced data; tag-injection risk).
 */
public class PapiIntegration implements PlaceholderIntegration {

    @Override
    public @NotNull String pluginName() {
        return "PlaceholderAPI";
    }

    @Override
    public void enable(@NotNull PlaceholderSource source, @NotNull PlaceholderService service, @NotNull uSkyBlock plugin) {
        new UskyblockExpansion(plugin, source).register();
        service.register(PapiIntegration::papiResolver);
    }

    static @NotNull TagResolver papiResolver(@NotNull Player viewer) {
        return TagResolver.resolver("papi", (args, ctx) -> {
            if (!args.hasNext()) {
                return Tag.selfClosingInserting(Component.text("<papi>"));
            }
            StringBuilder name = new StringBuilder(args.pop().value());
            while (args.hasNext()) {
                name.append(':').append(args.pop().value());
            }
            String token = "%" + name + "%";
            String replaced;
            try {
                replaced = PlaceholderAPI.setPlaceholders(viewer, token);
            } catch (RuntimeException e) {
                // A throwing third-party expansion must not abort our message rendering.
                replaced = token;
            }
            return Tag.selfClosingInserting(LegacyComponentSerializer.legacySection().deserialize(replaced));
        });
    }
}
