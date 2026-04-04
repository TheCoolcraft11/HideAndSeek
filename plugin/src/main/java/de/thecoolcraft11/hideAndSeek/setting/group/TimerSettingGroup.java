package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import de.thecoolcraft11.timer.AnimationType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TimerSettingGroup implements SettingGroup {

    private static final String[] DYE_NAMES = {
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    };
    private static final int[] DYE_RGB = {
            0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
            0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21
    };

    private static ItemStack colorIconFromHex(Object value, String suffix, Material fallback) {
        return new ItemStack(nearestColorMaterial(value, suffix, fallback));
    }

    private static Material nearestColorMaterial(Object value, String suffix, Material fallback) {
        int rgb = parseHexColor(value);
        if (rgb < 0) {
            return fallback;
        }

        int bestIndex = 0;
        long bestDistance = Long.MAX_VALUE;

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        for (int i = 0; i < DYE_RGB.length; i++) {
            int pr = (DYE_RGB[i] >> 16) & 0xFF;
            int pg = (DYE_RGB[i] >> 8) & 0xFF;
            int pb = DYE_RGB[i] & 0xFF;

            long dr = r - pr;
            long dg = g - pg;
            long db = b - pb;
            long distance = dr * dr + dg * dg + db * db;

            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        String materialName = DYE_NAMES[bestIndex] + "_" + suffix;
        Material material = Material.matchMaterial(materialName);
        return material == null ? fallback : material;
    }

    private static int parseHexColor(Object value) {
        if (value == null) {
            return -1;
        }

        String input = String.valueOf(value).trim();
        if (input.isEmpty()) {
            return -1;
        }

        if (input.startsWith("#")) {
            input = input.substring(1);
        }

        input = input.toUpperCase(Locale.ROOT);
        if (!input.matches("[0-9A-F]{6}")) {
            return -1;
        }

        try {
            return Integer.parseInt(input, 16);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("timer.hiding.primary-color", SettingType.STRING, String.class)
                        .defaultValue(resolver.get(plugin, "timer.hiding.primary-color", "#FF0000"))
                        .description("Primary color for hiding timer (hex code)")
                        .itemProvider(value -> colorIconFromHex(value, "CONCRETE_POWDER", Material.RED_CONCRETE_POWDER))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("timer.hiding.secondary-color", SettingType.STRING, String.class)
                        .defaultValue(resolver.get(plugin, "timer.hiding.secondary-color", "#0000FF"))
                        .description("Secondary color for hiding timer (hex code)")
                        .itemProvider(value -> colorIconFromHex(value, "CONCRETE", Material.BLUE_CONCRETE))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("timer.seeking.primary-color", SettingType.STRING, String.class)
                        .defaultValue(resolver.get(plugin, "timer.seeking.primary-color", "#FFFF00"))
                        .description("Primary color for seeking timer (hex code)")
                        .itemProvider(value -> colorIconFromHex(value, "WOOL", Material.YELLOW_WOOL))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("timer.seeking.secondary-color", SettingType.STRING, String.class)
                        .defaultValue(resolver.get(plugin, "timer.seeking.secondary-color", "#00FFFF"))
                        .description("Secondary color for seeking timer (hex code)")
                        .itemProvider(value -> colorIconFromHex(value, "STAINED_GLASS", Material.CYAN_STAINED_GLASS))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("timer.animation.type", SettingType.ENUM, AnimationType.class)
                        .defaultValue(resolver.getEnum(plugin, "timer.animation.type", AnimationType.class, AnimationType.WAVE))
                        .description("Timer animation type")
                        .customIcon(Material.AMETHYST_SHARD)
                        .valueIcons(Map.of(
                                AnimationType.WAVE, Material.WATER_BUCKET,
                                AnimationType.GRADIENT, Material.AMETHYST_BLOCK,
                                AnimationType.PULSE, Material.REDSTONE_LAMP
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("timer.animation.speed", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "timer.animation.speed", 0.5))
                        .range(0, 2)
                        .description("Timer animation speed (0.1 = slow, 2.0 = fast)")
                        .customIcon(Material.REDSTONE)
                        .itemProvider(value -> {
                            double speed = value instanceof Number number ? number.doubleValue() : 0.5;
                            if (speed < 0.75) {
                                return new ItemStack(Material.SOUL_TORCH);
                            }
                            if (speed < 1.5) {
                                return new ItemStack(Material.REDSTONE_TORCH);
                            }
                            return new ItemStack(Material.BLAZE_POWDER);
                        })
                        .build())
        );
    }
}
