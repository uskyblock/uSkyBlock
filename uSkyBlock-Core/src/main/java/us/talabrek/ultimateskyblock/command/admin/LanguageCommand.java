package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.PluginConfig;
import us.talabrek.ultimateskyblock.config.bootstrap.ConfigBootstrap;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.send;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

/**
 * Lists and configures plugin locale.
 */
public class LanguageCommand extends AbstractCommand {
    private static final int LOCALES_PER_LINE = 10;
    private final uSkyBlock plugin;
    private final PluginConfig pluginConfig;
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public LanguageCommand(@NotNull uSkyBlock plugin, @NotNull PluginConfig pluginConfig, @NotNull RuntimeConfigs runtimeConfigs) {
        super("lang|language", "usb.admin.config", "?locale", marktr("shows or sets the plugin language"));
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.runtimeConfigs = runtimeConfigs;
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
            sendErrorTr(sender, "Unsupported locale: <locale>", unparsed("locale", args[0]));
            sendTr(sender, "Use <cmd>/usb lang</cmd> to list all supported locales.", MUTED);
            sendTr(sender, "Help improve translations: <url>",
                MUTED, unparsed("url", I18nUtil.getTranslationSupportUrl(), PRIMARY));
            return true;
        }

        String localeKey = resolvedLocale.get();
        String current = runtimeConfigs.current().configuredLanguage();
        if (I18nUtil.findSupportedLocaleKey(current).orElse(current).equalsIgnoreCase(localeKey)) {
            sendTr(sender, "Language is already set to <locale>.",
                MUTED, unparsed("locale", localeKey, PRIMARY));
            sendTr(sender, "Help improve translations: <url>",
                MUTED, unparsed("url", I18nUtil.getTranslationSupportUrl(), PRIMARY));
            return true;
        }

        String previousLocale = pluginConfig.getYamlConfig().getString("language", ConfigBootstrap.DEFAULT_LANGUAGE);
        pluginConfig.getYamlConfig().set("language", localeKey);
        try {
            pluginConfig.save();
        } catch (IOException e) {
            pluginConfig.getYamlConfig().set("language", previousLocale);
            sendErrorTr(sender, "Unable to save config.yml: <reason>", unparsed("reason", e.getMessage()));
            plugin.getLogger().warning("Unable to save config.yml after language change: " + e.getMessage());
            return true;
        }
        RuntimeConfig runtimeConfig = runtimeConfigs.reload();
        I18nUtil.setLocale(runtimeConfig.locale());
        sendTr(sender, "Language updated to <locale>.",
            MUTED, unparsed("locale", localeKey, PRIMARY));
        sendTr(sender, "Use <cmd>/usb reload</cmd> to fully reload locale-aware config resources.", MUTED);
        sendTr(sender, "Help improve translations: <url>",
            MUTED, unparsed("url", I18nUtil.getTranslationSupportUrl(), PRIMARY));
        return true;
    }

    private void showLanguageOverview(@NotNull CommandSender sender) {
        List<String> supported = I18nUtil.getSupportedLocaleKeys();
        String current = runtimeConfigs.current().configuredLanguage();
        Optional<String> systemSupported = I18nUtil.resolveSupportedLocaleKey(Locale.getDefault());
        String currentSupported = I18nUtil.findSupportedLocaleKey(current).orElse(current);

        sendTr(sender, "Available locales (<count>):",
            MUTED, unparsed("count", String.valueOf(supported.size())));

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
        sendTr(sender, "Current language: <locale>", MUTED, unparsed("locale", current, PRIMARY));
        if (systemSupported.isPresent()) {
            sendTr(sender, "System locale suggestion: <locale>",
                MUTED, unparsed("locale", systemSupported.get(), PRIMARY));
        }
        sendTr(sender, "Set language with <cmd>/usb lang [locale]</cmd>.", MUTED);
        sendTr(sender, "Help improve translations: <url>",
            MUTED, unparsed("url", I18nUtil.getTranslationSupportUrl(), PRIMARY));
    }
}
