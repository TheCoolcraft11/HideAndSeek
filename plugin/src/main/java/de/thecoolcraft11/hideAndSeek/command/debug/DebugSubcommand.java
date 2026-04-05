package de.thecoolcraft11.hideAndSeek.command.debug;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public interface DebugSubcommand {
    static @NotNull List<String> filterByPrefix(@NotNull Collection<String> values, @NotNull String input) {
        String normalized = input.toLowerCase();
        return values.stream()
                .filter(value -> value != null && value.toLowerCase().startsWith(normalized))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    boolean handle(@NotNull CommandSender sender, @NotNull String[] args);

    default @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }
}


