package de.thecoolcraft11.hideAndSeek.setting.spec;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.SettingIconHelper;
import de.thecoolcraft11.hideAndSeek.setting.SettingValueResolver;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class DoubleSettingSpec implements SettingSpec {

    private final String key;

    private final double fallback;

    private final double min;

    private final double max;

    private final String description;

    private final Material icon;

    private final ItemStack iconStack;

    public DoubleSettingSpec(String key, double fallback, double min, double max, String description, Material icon) {
        this.key = key;
        this.fallback = fallback;
        this.min = min;
        this.max = max;
        this.description = description;
        this.iconStack = null;
        this.icon = icon;
    }

    public DoubleSettingSpec(String key, double fallback, double min, double max, String description, ItemStack iconStack) {
        this.key = key;
        this.fallback = fallback;
        this.min = min;
        this.max = max;
        this.description = description;
        this.icon = null;
        this.iconStack = iconStack;
    }

    @Override
    public void register(HideAndSeek plugin, SettingValueResolver resolver, SettingIconHelper iconHelper) {
        plugin.getConfigRegistry().register("settings." + key, Double.class, fallback);
        if (iconStack != null) {
            plugin.getSettingRegistry().register(
                    SettingDefinition.builder(key, SettingType.DOUBLE, Double.class).defaultValue(fallback).rangeDouble(
                            min,
                            max).description(description).customIcon(iconStack).build());
        } else {
            plugin.getSettingRegistry().register(
                    SettingDefinition.builder(key, SettingType.DOUBLE, Double.class).defaultValue(fallback).rangeDouble(
                            min,
                            max).description(description).customIcon(icon).build());
        }
    }
}
