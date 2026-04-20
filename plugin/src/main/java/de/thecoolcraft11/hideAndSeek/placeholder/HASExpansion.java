package de.thecoolcraft11.hideAndSeek.placeholder;

import com.google.gson.JsonElement;
import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.minigameframework.storage.sql.stats.MinigameStatsAPI;
import de.thecoolcraft11.minigameframework.storage.sql.stats.StatValue;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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


        String predefined = handleSimpleParams(player, params);
        if (predefined != null) return predefined;
        String coloredPredefined = handleSimpleColoredParams(player, params);
        if (coloredPredefined != null) return coloredPredefined;


        ParsedPlaceholder parsed = parseParams(params);

        StatValue value = MinigameStatsAPI.getCachedStat(
                player.getUniqueId(),
                minigameId,
                parsed.statKey
        );

        if (value == null) return parsed.defaultValue;

        if (parsed.path == null || parsed.path.isEmpty()) {
            return value.asString();
        }

        return resolveJsonPath(value, parsed.path, parsed.defaultValue);
    }


    private String handleSimpleParams(Player player, String params) {

        switch (params.toLowerCase()) {

            case "role": {
                String role = HideAndSeek.getDataController().getHiders().contains(
                        player.getUniqueId()) ? "Hider" : "Seeker";
                if (HideAndSeek.getActiveInstance().getStateManager().isPhase("lobby"))
                    role = "N/A";

                return role;
            }

            case "mode": {
                GameModeEnum mode = HideAndSeek.getActiveInstance().getSettingRegistry().get("game.mode");
                return switch (mode) {
                    case NORMAL -> "Normal";
                    case BLOCK -> "Block";
                    case SMALL -> "Small";
                };
            }

            case "points":
                return HideAndSeek.getDataController().getPoints(player.getUniqueId()) + "";

            case "players": {
                Set<UUID> players = new HashSet<>();
                players.addAll(HideAndSeek.getDataController().getHiders());
                players.addAll(HideAndSeek.getDataController().getHiders());
                return players.size() + "";
            }

            case "hiders":
                return HideAndSeek.getDataController().getHiders().size() + "";

            case "seekers":
                return HideAndSeek.getDataController().getSeekers().size() + "";

            default:
                return null;
        }
    }

    private String handleSimpleColoredParams(Player player, String params) {

        switch (params.toLowerCase()) {

            case "role_colored": {
                Team team = HideAndSeek.getActiveInstance().getTeamManager().getTeam(
                        HideAndSeek.getActiveInstance().getTeamManager().getPlayerTeam(player));
                TextColor color = team.color();
                String role = "<" + color.asHexString() + ">" + (HideAndSeek.getDataController().getHiders().contains(
                        player.getUniqueId()) ? "Hider" : "Seeker");
                if (HideAndSeek.getActiveInstance().getStateManager().isPhase("lobby"))
                    role = "<gray>N/A</gray>";

                return role;
            }

            case "mode": {
                GameModeEnum mode = HideAndSeek.getActiveInstance().getSettingRegistry().get("game.mode");
                return switch (mode) {
                    case NORMAL -> "<green>Normal</green>";
                    case BLOCK -> "<blue>Block</blue>";
                    case SMALL -> "<yellow>Small</yellow>";
                };
            }

            default:
                return null;
        }
    }

    private ParsedPlaceholder parseParams(String params) {

        String statKey;
        String path = null;
        String defaultValue = "0";

        int start = params.indexOf('[');
        int end = params.lastIndexOf(']');

        if (start != -1 && end > start) {

            statKey = params.substring(0, start);

            String inside = params.substring(start + 1, end);
            int split = inside.indexOf('|');

            if (split != -1) {
                path = inside.substring(0, split);
                defaultValue = inside.substring(split + 1);
            } else {
                path = inside;
            }

        } else {
            statKey = params;
        }

        return new ParsedPlaceholder(statKey, path, defaultValue);
    }

    private String resolveJsonPath(StatValue value, String path, String defaultValue) {

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
                return handleSize(current, defaultValue);
            }


            if (part.startsWith("contains(") && part.endsWith(")")) {
                return handleContains(current, part, defaultValue);
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

        return formatJson(current, defaultValue);
    }

    private String handleSize(JsonElement current, String defaultValue) {
        if (current.isJsonArray()) return String.valueOf(current.getAsJsonArray().size());
        if (current.isJsonObject()) return String.valueOf(current.getAsJsonObject().size());
        return defaultValue;
    }

    private String handleContains(JsonElement current, String part, String defaultValue) {

        if (!current.isJsonArray()) return defaultValue;

        String search = part.substring(9, part.length() - 1);

        for (JsonElement el : current.getAsJsonArray()) {
            if (el != null && el.isJsonPrimitive()) {
                if (el.getAsString().equalsIgnoreCase(search)) {
                    return "true";
                }
            }
        }

        return "false";
    }

    private String formatJson(JsonElement current, String defaultValue) {

        if (current == null || current.isJsonNull()) {
            return defaultValue;
        }

        if (current.isJsonPrimitive()) {
            return current.getAsString();
        }

        return current.toString();
    }

    private static class ParsedPlaceholder {
        String statKey;
        String path;
        String defaultValue;

        ParsedPlaceholder(String statKey, String path, String defaultValue) {
            this.statKey = statKey;
            this.path = path;
            this.defaultValue = defaultValue;
        }
    }
}