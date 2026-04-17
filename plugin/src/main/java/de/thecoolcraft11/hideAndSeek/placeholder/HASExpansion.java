package de.thecoolcraft11.hideAndSeek.placeholder;

import com.google.gson.JsonElement;
import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.storage.sql.stats.MinigameStatsAPI;
import de.thecoolcraft11.minigameframework.storage.sql.stats.StatValue;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class HASExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "has";
    }

    @Override
    public @NotNull String getAuthor() {
        return "THeCoolcraft11";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {

        if (player == null) return "0";

        String minigameId = HideAndSeek.getActiveInstance().getName();

        int bracketStart = params.indexOf('[');
        int bracketEnd = params.lastIndexOf(']');

        String statKey;
        String path = null;
        String defaultValue = "0";

        if (bracketStart != -1 && bracketEnd != -1 && bracketEnd > bracketStart) {

            statKey = params.substring(0, bracketStart);
            String inside = params.substring(bracketStart + 1, bracketEnd);

            int defaultIndex = inside.indexOf('|');
            if (defaultIndex != -1) {
                path = inside.substring(0, defaultIndex);
                defaultValue = inside.substring(defaultIndex + 1);
            } else {
                path = inside;
            }

        } else {
            statKey = params;
        }

        StatValue value = MinigameStatsAPI.getCachedStat(
                player.getUniqueId(),
                minigameId,
                statKey
        );

        if (value == null) return defaultValue;

        if (path == null || path.isEmpty()) {
            return value.asString();
        }

        JsonElement current;

        try {
            current = value.asJson();
        } catch (Exception e) {
            return defaultValue;
        }

        String[] parts = path.split("\\.");

        for (String part : parts) {

            if (current == null || current.isJsonNull()) {
                return defaultValue;
            }

            if (part.equalsIgnoreCase("size")) {
                if (current.isJsonArray()) {
                    return String.valueOf(current.getAsJsonArray().size());
                }
                if (current.isJsonObject()) {
                    return String.valueOf(current.getAsJsonObject().size());
                }
                return defaultValue;
            }

            if (part.startsWith("contains(") && part.endsWith(")")) {

                String search = part.substring(9, part.length() - 1);

                if (!current.isJsonArray()) {
                    return defaultValue;
                }

                for (JsonElement el : current.getAsJsonArray()) {
                    if (el != null && el.isJsonPrimitive()) {
                        if (el.getAsString().equalsIgnoreCase(search)) {
                            return "true";
                        }
                    }
                }

                return "false";
            }

            if (current.isJsonArray()) {
                try {
                    int index = Integer.parseInt(part);
                    current = current.getAsJsonArray().get(index);
                    continue;
                } catch (Exception e) {
                    return defaultValue;
                }
            }

            if (current.isJsonObject()) {
                current = current.getAsJsonObject().get(part);
                continue;
            }

            return defaultValue;
        }

        if (current == null || current.isJsonNull()) {
            return defaultValue;
        }

        if (current.isJsonPrimitive()) {
            return current.getAsString();
        }

        return current.toString();
    }
}