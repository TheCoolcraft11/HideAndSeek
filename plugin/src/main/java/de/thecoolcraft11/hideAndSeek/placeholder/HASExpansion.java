package de.thecoolcraft11.hideAndSeek.placeholder;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService;
import de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService.PlayerStatsRecord;
import de.thecoolcraft11.minigameframework.storage.sql.stats.MinigameStatsAPI;
import de.thecoolcraft11.minigameframework.storage.sql.stats.StatValue;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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


    private static final char[] SMALL_CAPS_MAP = new char[26];

    static {
        SMALL_CAPS_MAP[0] = 'ᴀ';
        SMALL_CAPS_MAP[1] = 'ʙ';
        SMALL_CAPS_MAP[2] = 'ᴄ';
        SMALL_CAPS_MAP[3] = 'ᴅ';
        SMALL_CAPS_MAP[4] = 'ᴇ';
        SMALL_CAPS_MAP[5] = 'ꜰ';
        SMALL_CAPS_MAP[6] = 'ɢ';
        SMALL_CAPS_MAP[7] = 'ʜ';
        SMALL_CAPS_MAP[8] = 'ɪ';
        SMALL_CAPS_MAP[9] = 'ᴊ';
        SMALL_CAPS_MAP[10] = 'ᴋ';
        SMALL_CAPS_MAP[11] = 'ʟ';
        SMALL_CAPS_MAP[12] = 'ᴍ';
        SMALL_CAPS_MAP[13] = 'ɴ';
        SMALL_CAPS_MAP[14] = 'ᴏ';
        SMALL_CAPS_MAP[15] = 'ᴘ';
        SMALL_CAPS_MAP[16] = 'ǫ';
        SMALL_CAPS_MAP[17] = 'ʀ';
        SMALL_CAPS_MAP[18] = 's';
        SMALL_CAPS_MAP[19] = 'ᴛ';
        SMALL_CAPS_MAP[20] = 'ᴜ';
        SMALL_CAPS_MAP[21] = 'ᴠ';
        SMALL_CAPS_MAP[22] = 'ᴡ';
        SMALL_CAPS_MAP[23] = 'x';
        SMALL_CAPS_MAP[24] = 'ʏ';
        SMALL_CAPS_MAP[25] = 'ᴢ';
    }

    private static String toSmallCaps(String input) {
        char[] chars = input.toLowerCase().toCharArray();
        StringBuilder out = new StringBuilder(chars.length);

        boolean insideTag = false;

        for (char c : chars) {
            if (c == '<') {
                insideTag = true;
                out.append(c);
                continue;
            }

            if (c == '>') {
                insideTag = false;
                out.append(c);
                continue;
            }

            if (insideTag) {
                out.append(c);
                continue;
            }

            if (c >= 'a' && c <= 'z') {
                out.append(SMALL_CAPS_MAP[c - 'a']);
            } else {
                out.append(c);
            }
        }

        return out.toString();
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
                    case SKIN -> "Skin";
                };
            }

            case "points":
                return HideAndSeek.getDataController().getPoints(player.getUniqueId()) + "";

            case "players": {
                if (HideAndSeek.getActiveInstance().getStateManager().isPhase("lobby")) {
                    return Bukkit.getOnlinePlayers().size() + "";
                }
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

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {

        if (player == null) return "0";

        boolean smallCaps = params.contains("|sc");

        params = params.replaceFirst("\\|sc$", "");

        String minigameId = HideAndSeek.getActiveInstance().getName();


        String predefined = handleSimpleParams(player, params);
        if (predefined != null) return smallCaps ? toSmallCaps(predefined) : predefined;
        String coloredPredefined = handleSimpleColoredParams(player, params);
        if (coloredPredefined != null) return smallCaps ? toSmallCaps(coloredPredefined) : coloredPredefined;


        ParsedPlaceholder parsed = parseParams(params);


        if (parsed.statKey.equalsIgnoreCase("stats")) {
            PlayerStatsService statsService = PlayerStatsService.getActive();
            if (statsService == null) return parsed.defaultValue;
            PlayerStatsRecord record = statsService.getSnapshot(player.getUniqueId());
            if (record == null) return parsed.defaultValue;

            JsonElement root = JsonParser.parseString(new Gson().toJson(record));

            if (parsed.path == null || parsed.path.isEmpty()) {
                String out = root.isJsonPrimitive() ? root.getAsString() : root.toString();
                return smallCaps ? toSmallCaps(out) : out;
            }

            return smallCaps ? toSmallCaps(resolveJsonPathFromElement(root, parsed.path, parsed.defaultValue))
                    : resolveJsonPathFromElement(root, parsed.path, parsed.defaultValue);
        }


        StatValue value = MinigameStatsAPI.getCachedStat(
                player.getUniqueId(),
                minigameId,
                parsed.statKey
        );

        if (value == null) {
            try {
                String canonical = de.thecoolcraft11.hideAndSeek.playerdata.DatabasePlayerDataStore.MINIGAME_ID;
                if (!canonical.equals(minigameId)) {
                    value = MinigameStatsAPI.getCachedStat(player.getUniqueId(), canonical, parsed.statKey);
                }
            } catch (Exception ignored) {
            }
        }

        if (value == null) return parsed.defaultValue;

        if (parsed.path == null || parsed.path.isEmpty()) {
            return smallCaps ? toSmallCaps(value.asString()) : value.asString();
        }

        return smallCaps ? toSmallCaps(resolveJsonPath(value, parsed.path, parsed.defaultValue)) : resolveJsonPath(
                value, parsed.path, parsed.defaultValue);
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


            if (part.equalsIgnoreCase("human") || part.equalsIgnoreCase("duration") || part.equalsIgnoreCase("time")) {
                return formatMillisHumanReadable(current, defaultValue);
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

    private String resolveJsonPathFromElement(JsonElement root, String path, String defaultValue) {

        JsonElement current = root;

        String[] parts = path.split("\\.");

        for (String part : parts) {

            if (current == null || current.isJsonNull()) {
                return defaultValue;
            }

            if (part.equalsIgnoreCase("size")) {
                if (current.isJsonArray()) return String.valueOf(current.getAsJsonArray().size());
                if (current.isJsonObject()) return String.valueOf(current.getAsJsonObject().size());
                return defaultValue;
            }

            if (part.equalsIgnoreCase("human") || part.equalsIgnoreCase("duration") || part.equalsIgnoreCase("time")) {
                return formatMillisHumanReadable(current, defaultValue);
            }

            if (part.startsWith("contains(") && part.endsWith(")")) {
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

        if (current == null || current.isJsonNull()) return defaultValue;
        if (current.isJsonPrimitive()) return current.getAsString();
        return current.toString();
    }

    private String handleSize(JsonElement current, String defaultValue) {
        if (current.isJsonArray()) return String.valueOf(current.getAsJsonArray().size());
        if (current.isJsonObject()) return String.valueOf(current.getAsJsonObject().size());
        return defaultValue;
    }

    private String formatMillisHumanReadable(JsonElement current, String defaultValue) {

        if (current == null || current.isJsonNull()) {
            return defaultValue;
        }

        long millis;
        try {
            if (current.isJsonPrimitive()) {
                millis = current.getAsLong();
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }

        long seconds = Math.max(0L, millis / 1000L);
        if (seconds < 60) {
            return seconds + "s";
        }
        if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        }
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
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

    private String handleSimpleColoredParams(Player player, String params) {

        switch (params.toLowerCase()) {

            case "role_colored": {
                Team team = HideAndSeek.getActiveInstance().getTeamManager().getTeam(
                        HideAndSeek.getActiveInstance().getTeamManager().getPlayerTeam(player));
                TextColor color = team.color();
                String role = "<" + color.asHexString() + ">" + (HideAndSeek.getDataController().getHiders().contains(
                        player.getUniqueId()) ? "Hider" : "Seeker");
                if (HideAndSeek.getActiveInstance().getStateManager().isPhase("lobby"))
                    role = "<#696969>N/A</#696969>";

                return role;
            }

            case "mode_colored": {
                GameModeEnum mode = HideAndSeek.getActiveInstance().getSettingRegistry().get("game.mode");
                return switch (mode) {
                    case NORMAL -> "<green>Normal</green>";
                    case BLOCK -> "<blue>Block</blue>";
                    case SMALL -> "<yellow>Small</yellow>";
                    case SKIN -> "<red>Skin</red>";
                };
            }

            default:
                return null;
        }
    }

}