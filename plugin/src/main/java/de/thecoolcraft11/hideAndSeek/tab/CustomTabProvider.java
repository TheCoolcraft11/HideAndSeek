package de.thecoolcraft11.hideAndSeek.tab;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomTabProvider {
    private final FileConfiguration config;
    private final Map<String, List<TextColor>> colorGradients = new HashMap<>();
    private final Map<String, Double> colorSpeeds = new HashMap<>();
    private final boolean enableHeader;
    private final boolean enableFooter;
    private int animationTick = 0;

    public CustomTabProvider(HideAndSeek plugin, FileConfiguration config) {
        this.config = config;
        this.enableHeader = config.getBoolean("tab.enable-header", true);
        this.enableFooter = config.getBoolean("tab.enable-footer", true);


        loadColorGradients();
        loadColorSpeeds();

        Bukkit.getScheduler().runTaskTimer(plugin, () -> animationTick++, 1L, 1L);
    }

    private void loadColorGradients() {

        loadColors(config.getStringList("tab.header.title-colors"), "title-colors", 0xFF0000, 0xFFFF00);

        loadColors(config.getStringList("tab.header.ip-colors"), "ip-colors", 0x00FF00, 0x00FFFF);
    }

    private void loadColors(List<String> hexStrings, String key, int def1, int def2) {
        List<TextColor> colors = new ArrayList<>();
        if (hexStrings.isEmpty()) {
            colors.add(TextColor.color(def1));
            colors.add(TextColor.color(def2));
        } else {
            for (String hex : hexStrings) {
                colors.add(TextColor.fromHexString(hex));
            }
        }
        colorGradients.put(key, colors);
    }

    private void loadColorSpeeds() {
        colorSpeeds.put("title-speed", config.getDouble("tab.header.title-speed", 0.5));
        colorSpeeds.put("ip-speed", config.getDouble("tab.header.ip-speed", 0.3));
    }

    public void updateTab(Player player) {

        String role = HideAndSeek.getDataController().getHiders().contains(player.getUniqueId()) ? "Hider" : "Seeker";
        if (HideAndSeek.getActiveInstance().getStateManager().isPhase("lobby")) role = "N/A";

        int total = Bukkit.getOnlinePlayers().size();
        int h = HideAndSeek.getDataController().getHiders().size();
        int s = HideAndSeek.getDataController().getSeekers().size();
        int p = HideAndSeek.getDataController().getPoints(player.getUniqueId());
        int c = ItemSkinSelectionService.getCoins(player.getUniqueId());
        GameModeEnum mode = HideAndSeek.getActiveInstance().getSettingRegistry().get("game.mode");

        String modeText = switch (mode) {
            case NORMAL -> "Normal";
            case BLOCK -> "Block";
            case SMALL -> "Small";
        };


        Map<String, Object> context = new HashMap<>();
        context.put("player", player.getName());
        context.put("role", role);
        context.put("mode", modeText);
        context.put("server-ip", config.getString("tab.server-ip", "yourserver.com"));
        context.put("players-total", total);
        context.put("players-hiders", h);
        context.put("players-seekers", s);
        context.put("points", p);
        context.put("coins", c);
        context.put("role-color", getRoleColor(role));

        Component header = buildHeader(context);
        Component footer = buildFooter(context);

        if (enableHeader) player.sendPlayerListHeader(header);
        if (enableFooter) player.sendPlayerListFooter(footer);
    }

    private TextColor getRoleColor(String role) {
        return switch (role) {
            case "N/A" -> NamedTextColor.GRAY;
            case "Hider" -> NamedTextColor.BLUE;
            case "Seeker" -> NamedTextColor.RED;
            default -> NamedTextColor.WHITE;
        };
    }

    private Component buildHeader(Map<String, Object> context) {
        var elements = config.getList("tab.header.elements");
        if (elements == null) {
            return Component.text("Header not configured");
        }

        TextComponent.Builder builder = Component.text();

        for (Object element : elements) {
            if (!(element instanceof Map<?, ?> elementMap)) continue;

            Object enabledObj = elementMap.get("enabled");
            boolean enabled = enabledObj == null ? true : (boolean) enabledObj;
            if (!enabled) continue;

            String text = (String) elementMap.get("text");
            if (text == null) continue;


            String processedText = replacePlaceholders(text, context);


            Object animatedObj = elementMap.get("animated");
            boolean animated = animatedObj == null ? false : (boolean) animatedObj;

            if (animated) {
                String colorListKey = (String) elementMap.get("color-list");
                String speedKey = (String) elementMap.get("speed");
                List<TextColor> colors = colorGradients.getOrDefault(colorListKey, colorGradients.get("title-colors"));
                double speed = colorSpeeds.getOrDefault(speedKey, 0.5);

                builder.append(buildWaveText(processedText, speed, colors));
            } else {
                String colorValue = (String) elementMap.get("color");
                TextColor color = parseColor(colorValue, context, NamedTextColor.WHITE);
                builder.append(Component.text(processedText, color));
            }
        }

        return builder.build();
    }

    private Component buildFooter(Map<String, Object> context) {
        var elements = config.getList("tab.footer.elements");
        if (elements == null) {
            return Component.text("Footer not configured");
        }

        TextComponent.Builder builder = Component.text();

        for (Object element : elements) {
            if (!(element instanceof Map<?, ?> elementMap)) continue;

            Object enabledObj = elementMap.get("enabled");
            boolean enabled = enabledObj == null ? true : (boolean) enabledObj;
            if (!enabled) continue;

            String text = (String) elementMap.get("text");
            if (text == null) continue;


            String processedText = replacePlaceholders(text, context);
            String colorValue = (String) elementMap.get("color");
            TextColor color = parseColor(colorValue, context, NamedTextColor.WHITE);

            builder.append(Component.text(processedText, color));
        }

        return builder.build();
    }

    private String replacePlaceholders(String text, Map<String, Object> context) {
        String result = text;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    private TextColor parseColor(String colorValue, Map<String, Object> context, TextColor defaultColor) {
        if (colorValue == null) return defaultColor;


        if (colorValue.startsWith("{") && colorValue.endsWith("}")) {
            String key = colorValue.substring(1, colorValue.length() - 1);
            Object colorObj = context.get(key);
            if (colorObj instanceof TextColor) {
                return (TextColor) colorObj;
            }
            return defaultColor;
        }


        if (colorValue.startsWith("#")) {
            return TextColor.fromHexString(colorValue);
        }

        return defaultColor;
    }

    private Component buildWaveText(String text, double spread, List<TextColor> colorList) {
        TextComponent.Builder builder = Component.text();

        for (int i = 0; i < text.length(); i++) {
            double t = (i * spread) + (animationTick * 0.2);
            float ratio = (float) ((Math.sin(t) + 1) / 2);

            builder.append(Component.text(text.charAt(i), interpolateColor(ratio, colorList)));
        }
        return builder.build();
    }

    private TextColor interpolateColor(float ratio, List<TextColor> list) {
        if (list.size() < 2) return list.getFirst();

        float sector = ratio * (list.size() - 1);
        int index = (int) sector;
        float localRatio = sector - index;

        if (index >= list.size() - 1) return list.getLast();

        return TextColor.lerp(localRatio, list.get(index), list.get(index + 1));
    }
}
