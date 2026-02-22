package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.send;

/**
 * Lists and configures plugin locale.
 */
public class LanguageCommand extends AbstractCommand {
    private static final String DEFAULT_LOCALE_KEY = "en";
    private static final int LOCALES_PER_LINE = 10;
    private final uSkyBlock plugin;

    @Inject
    public LanguageCommand(@NotNull uSkyBlock plugin) {
        super("lang|language", "usb.admin.config", "?locale", marktr("shows or sets the plugin language"));
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        if (args.length == 0) {
            showLanguageOverview(sender);
            return true;
        } else if (args.length != 1) {
            return false;
        }

        Optional<String> resolvedLocale = I18nUtil.resolveSupportedLocaleKey(args[0]);
        if (resolvedLocale.isEmpty()) {
            send(sender, tr("<error>Unsupported locale: <locale>", unparsed("locale", args[0])));
            send(sender, tr("<muted>Use <cmd>/usb lang</cmd> to list all supported locales.</muted>"));
            send(sender, tr("<muted>Help improve translations: <primary><url></primary></muted>",
                unparsed("url", I18nUtil.getTranslationSupportUrl())));
            return true;
        }

        String localeKey = resolvedLocale.get();
        String current = plugin.getConfig().getString("language", DEFAULT_LOCALE_KEY);
        if (I18nUtil.findSupportedLocaleKey(current).orElse(current).equalsIgnoreCase(localeKey)) {
            send(sender, tr("<muted>Language is already set to <primary><locale></primary>.</muted>",
                unparsed("locale", localeKey)));
            send(sender, tr("<muted>Help improve translations: <primary><url></primary></muted>",
                unparsed("url", I18nUtil.getTranslationSupportUrl())));
            return true;
        }

        plugin.getConfig().set("language", localeKey);
        plugin.saveConfig();
        Settings.locale = I18nUtil.getLocale(localeKey);
        I18nUtil.setLocale(Settings.locale);
        send(sender, tr("<secondary>Language updated to <primary><locale></primary>.</secondary>",
            unparsed("locale", localeKey)));
        send(sender, tr("<muted>Use <cmd>/usb reload</cmd> to fully reload locale-aware config resources.</muted>"));
        send(sender, tr("<muted>Help improve translations: <primary><url></primary></muted>",
            unparsed("url", I18nUtil.getTranslationSupportUrl())));
        return true;
    }

    private void showLanguageOverview(@NotNull CommandSender sender) {
        List<String> supported = I18nUtil.getSupportedLocaleKeys();
        String current = plugin.getConfig().getString("language", DEFAULT_LOCALE_KEY);
        Optional<String> systemSupported = I18nUtil.resolveSupportedLocaleKey(Locale.getDefault());
        String currentSupported = I18nUtil.findSupportedLocaleKey(current).orElse(current);

        send(sender, tr("<primary>Available locales (<count>):</primary>",
            unparsed("count", String.valueOf(supported.size()))));

        for (int i = 0; i < supported.size(); i += LOCALES_PER_LINE) {
            int end = Math.min(i + LOCALES_PER_LINE, supported.size());
            Component line = Component.text(" - ", NamedTextColor.GRAY);
            for (int j = i; j < end; j++) {
                String locale = supported.get(j);
                boolean active = locale.equalsIgnoreCase(currentSupported);
                boolean system = systemSupported.map(locale::equalsIgnoreCase).orElse(false);
                NamedTextColor color = active ? NamedTextColor.AQUA : (system ? NamedTextColor.GREEN : NamedTextColor.GRAY);
                line = line.append(Component.text(locale, color));
                if (j + 1 < end) {
                    line = line.append(Component.text(", ", NamedTextColor.GRAY));
                }
            }
            send(sender, line);
        }
        send(sender, tr("<muted>Current language: <primary><locale></primary></muted>", unparsed("locale", current)));
        if (systemSupported.isPresent()) {
            send(sender, tr("<muted>System locale suggestion: <primary><locale></primary></muted>",
                unparsed("locale", systemSupported.get())));
        }
        send(sender, tr("<muted>Set language with <cmd>/usb lang [locale]</cmd>.</muted>"));
        send(sender, tr("<muted>Help improve translations: <primary><url></primary></muted>",
            unparsed("url", I18nUtil.getTranslationSupportUrl())));
    }
}
