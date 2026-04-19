package de.thecoolcraft11.hideAndSeek.tab;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.manager.ScoreboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CustomScoreboardProvider {

    private static final String TEMPLATE_ID = "hideandseek_main";
    private final FileConfiguration config;
    private final boolean smallCaps;
    private final boolean underline;
    private final boolean paddingEnabled;
    private final int paddingAmount;
    private final boolean titleAnimation;
    private final List<TextColor> gradientColors = new ArrayList<>();
    private final ScoreboardManager scoreboardManager;
    private int animationTick = 0;

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


    public CustomScoreboardProvider(HideAndSeek plugin) {
        this.scoreboardManager = plugin.getScoreboardManager();
        this.config = plugin.getConfig();

        this.smallCaps = config.getBoolean("scoreboard.title.small-caps");
        this.underline = config.getBoolean("scoreboard.title.underline");
        this.paddingEnabled = config.getBoolean("scoreboard.formatting.padding");
        this.paddingAmount = config.getInt("scoreboard.formatting.padding-amount");
        this.titleAnimation = config.getBoolean("scoreboard.animation.enabled");

        loadGradient();

        Bukkit.getScheduler().runTaskTimer(
                HideAndSeek.getActiveInstance(),
                () -> animationTick++,
                1L,
                1L
        );

        registerTemplate();
    }

    private static String toSmallCaps(String input) {
        char[] chars = input.toLowerCase().toCharArray();
        StringBuilder out = new StringBuilder(chars.length);

        for (char c : chars) {
            if (c >= 'a' && c <= 'z') {
                out.append(SMALL_CAPS_MAP[c - 'a']);
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }

    private void registerTemplate() {
        scoreboardManager.registerTemplate(TEMPLATE_ID, this::buildLines);
    }

    public void apply(Player player) {
        if (!scoreboardManager.hasScoreboard(player)) {
            scoreboardManager.createScoreboard(player,
                    buildTitle().style(Style.style().color(TextColor.color(0xFFFFFF)).decorate(TextDecoration.UNDERLINED).build()));
        }

        scoreboardManager.applyTemplate(player, TEMPLATE_ID);
    }

    public void unapply(Player player) {
        scoreboardManager.removeScoreboard(player);
    }

    public void startAutoUpdate(int intervalTicks) {
        scoreboardManager.startAutoUpdate(TEMPLATE_ID, intervalTicks);
    }

    public void updateTitle(Player player) {
        scoreboardManager.updateTitle(player, buildGradientTitle(toSmallCaps("HideAndSeek"), animationTick));
    }

    private List<Component> buildLines(Player player) {
        List<Component> lines = new ArrayList<>();


        var context = buildPlaceholderContext(player);

        var linesList = config.getList("scoreboard.lines");
        if (linesList != null) {
            for (Object element : linesList) {
                if (!(element instanceof java.util.Map<?, ?> elementMap)) continue;

                Object enabledObj = elementMap.get("enabled");
                boolean enabled = enabledObj == null || (boolean) enabledObj;
                if (!enabled) continue;

                String text = (String) elementMap.get("text");
                if (text == null) continue;

                String colorValue = (String) elementMap.get("color");
                if (colorValue == null) colorValue = "#FFFFFF";


                if (text.contains("{players-total}") && text.contains("{players-hiders}") && text.contains("{players-seekers}") && colorValue.contains(",")) {
                    lines.add(buildMultiColorPlayerCountLineWithColors(text, context, colorValue));
                    continue;
                }


                String processedText = replacePlaceholders(text, context);
                TextColor color = parseColor(colorValue, context, TextColor.fromHexString("#FFFFFF"));

                lines.add(Component.text(pad(toSmallCaps(processedText)), color));
            }
        }

        return lines;
    }

    private java.util.Map<String, Object> buildPlaceholderContext(Player player) {
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        context.put("player", player.getName());

        String role = de.thecoolcraft11.hideAndSeek.HideAndSeek.getDataController().getHiders().contains(player.getUniqueId()) ? "Hider" : "Seeker";
        if (de.thecoolcraft11.hideAndSeek.HideAndSeek.getActiveInstance().getStateManager().isPhase("lobby"))
            role = "N/A";
        int total = org.bukkit.Bukkit.getOnlinePlayers().size();
        int h = de.thecoolcraft11.hideAndSeek.HideAndSeek.getDataController().getHiders().size();
        int s = de.thecoolcraft11.hideAndSeek.HideAndSeek.getDataController().getSeekers().size();
        int p = de.thecoolcraft11.hideAndSeek.HideAndSeek.getDataController().getPoints(player.getUniqueId());
        int c = de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService.getCoins(player.getUniqueId());
        de.thecoolcraft11.hideAndSeek.model.GameModeEnum mode = de.thecoolcraft11.hideAndSeek.HideAndSeek.getActiveInstance().getSettingRegistry().get("game.mode");
        String modeText = switch (mode) {
            case NORMAL -> "Normal";
            case BLOCK -> "Block";
            case SMALL -> "Small";
        };

        context.put("role", role);
        context.put("mode", modeText);
        context.put("server-ip", config.getString("tab.server-ip", "yourserver.com"));
        context.put("players-total", total);
        context.put("players-hiders", h);
        context.put("players-seekers", s);
        context.put("points", p);
        context.put("coins", c);
        context.put("role-color", getRoleColor(role));
        context.put("players", total);
        return context;
    }

    private String replacePlaceholders(String text, java.util.Map<String, Object> context) {
        String result = text;
        for (java.util.Map.Entry<String, Object> entry : context.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    private TextColor parseColor(String colorValue, java.util.Map<String, Object> context, TextColor defaultColor) {
        if (colorValue == null) return defaultColor;

        if (colorValue.startsWith("{") && colorValue.endsWith("}")) {
            String key = colorValue.substring(1, colorValue.length() - 1);
            Object colorObj = context.get(key);
            if (colorObj instanceof TextColor tc) {
                return tc;
            }
            return defaultColor;
        }

        if (colorValue.startsWith("#")) {
            return TextColor.fromHexString(colorValue);
        }
        return defaultColor;
    }

    private TextColor getRoleColor(String role) {
        return switch (role) {
            case "N/A" -> net.kyori.adventure.text.format.NamedTextColor.GRAY;
            case "Hider" -> net.kyori.adventure.text.format.NamedTextColor.BLUE;
            case "Seeker" -> net.kyori.adventure.text.format.NamedTextColor.RED;
            default -> net.kyori.adventure.text.format.NamedTextColor.WHITE;
        };
    }

    private Component buildTitle() {
        String text = config.getString("scoreboard.title.text", "HideAndSeek");

        if (smallCaps) {
            text = toSmallCaps(text);
        }

        Component result = titleAnimation
                ? buildGradientTitle(text, animationTick)
                : Component.text(text);

        if (underline) {
            result = result.style(Style.style().decorate(TextDecoration.UNDERLINED).build());
        }

        return result;
    }

    private Component buildGradientTitle(String text, int tick) {

        TextColor[] colors;
        if (gradientColors.isEmpty()) {
            colors = new TextColor[]{
                    TextColor.color(0xFF5555),
                    TextColor.color(0xFFAA00),
                    TextColor.color(0xFFFF55),
                    TextColor.color(0x55FF55),
                    TextColor.color(0x55FFFF),
                    TextColor.color(0x5555FF),
                    TextColor.color(0xAA55FF)
            };
        } else {
            colors = gradientColors.toArray(new TextColor[0]);
        }

        ComponentBuilder<TextComponent, TextComponent.Builder> builder = Component.text();

        for (int i = 0; i < text.length(); i++) {

            double offset = (i * 0.35) + (tick * 0.15);

            float t = (float) ((Math.sin(offset) + 1) / 2);

            int idx1 = (int) (t * (colors.length - 1));
            int idx2 = Math.min(idx1 + 1, colors.length - 1);

            float local = (t * (colors.length - 1)) - idx1;

            TextColor color = lerp(colors[idx1], colors[idx2], local);

            builder.append(Component.text(text.charAt(i), color));
        }

        return builder.build();
    }

    private TextColor lerp(TextColor a, TextColor b, float t) {
        int r = (int) (a.red() + (b.red() - a.red()) * t);
        int g = (int) (a.green() + (b.green() - a.green()) * t);
        int b2 = (int) (a.blue() + (b.blue() - a.blue()) * t);

        return TextColor.color(r, g, b2);
    }

    private String pad(String input) {
        if (!paddingEnabled) return input;

        return input + " ".repeat(paddingAmount);
    }

    private void loadGradient() {
        List<String> colors = config.getStringList("scoreboard.colors.gradient");

        for (String hex : colors) {
            gradientColors.add(TextColor.fromHexString(hex));
        }

        if (gradientColors.isEmpty()) {
            gradientColors.add(TextColor.color(0xFF5555));
            gradientColors.add(TextColor.color(0xAA55FF));
        }
    }





    private Component buildMultiColorPlayerCountLineWithColors(String template, java.util.Map<String, Object> context, String colorList) {

        String total = String.valueOf(context.getOrDefault("players-total", "0"));
        String hiders = String.valueOf(context.getOrDefault("players-hiders", "0"));
        String seekers = String.valueOf(context.getOrDefault("players-seekers", "0"));


        String[] colors = colorList.split(",");
        TextColor colorPrefix = colors.length > 0 ? TextColor.fromHexString(colors[0].trim()) : TextColor.fromHexString("#A9A9A9");
        TextColor colorTotal = colors.length > 1 ? TextColor.fromHexString(colors[1].trim()) : TextColor.fromHexString("#FFFFFF");
        TextColor colorHiders = colors.length > 2 ? TextColor.fromHexString(colors[2].trim()) : TextColor.fromHexString("#00FF00");
        TextColor colorSeekers = colors.length > 3 ? TextColor.fromHexString(colors[3].trim()) : TextColor.fromHexString("#FF0000");


        int totalIdx = template.indexOf("{players-total}");
        String prefix = totalIdx > 0 ? template.substring(0, totalIdx) : "";
        String suffix = template.substring(totalIdx + "{players-total}".length());


        int hidersIdx = suffix.indexOf("{players-hiders}");
        String beforeHiders = hidersIdx > 0 ? suffix.substring(0, hidersIdx) : "";
        String afterHiders = suffix.substring(hidersIdx + "{players-hiders}".length());
        int seekersIdx = afterHiders.indexOf("{players-seekers}");
        String betweenHidersSeekers = seekersIdx > 0 ? afterHiders.substring(0, seekersIdx) : "";
        String afterSeekers = afterHiders.substring(seekersIdx + "{players-seekers}".length());


        TextComponent.Builder builder = Component.text();
        builder.append(Component.text(pad(toSmallCaps(prefix)), colorPrefix));
        builder.append(Component.text(total, colorTotal));
        builder.append(Component.text(beforeHiders, colorPrefix));
        builder.append(Component.text(hiders, colorHiders));
        builder.append(Component.text(betweenHidersSeekers, colorPrefix));
        builder.append(Component.text(seekers, colorSeekers));
        builder.append(Component.text(afterSeekers, colorPrefix));
        return builder.build();
    }
}
