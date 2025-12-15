package dk.lockfuglsang.minecraft.command.completion;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Common ancestor of the TabCompleters.
 * Uses the Template Pattern for subclasses.
 */
public abstract class AbstractTabCompleter implements TabCompleter {

    abstract protected List<String> getTabList(CommandSender commandSender, String term);

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, String[] args) {
        String term = args.length > 0 ? args[args.length - 1] : "";
        return filter(getTabList(commandSender, term), term);
    }

    @Contract(pure = true)
    public static @NotNull List<String> filter(@NotNull Collection<String> candidates, @NotNull String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .limit(20)
            .collect(Collectors.toCollection(ArrayList::new)); // explicitly return a mutable list in case legacy code has side effects
    }
}
