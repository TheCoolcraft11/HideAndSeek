package de.thecoolcraft11.hideAndSeek.setting.spec;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.SettingIconHelper;
import de.thecoolcraft11.hideAndSeek.setting.SettingValueResolver;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

public final class FloatSettingSpec implements SettingSpec {

    private final String key;
    private final float fallback;
    private final float min;
    private final float max;
    private final String description;
    private final Material icon;

    public FloatSettingSpec(String key, float fallback, float min, float max, String description, Material icon) {
        this.key = key;
        this.fallback = fallback;
        this.min = min;
        this.max = max;
        this.description = description;
        this.icon = icon;
    }

    @Override
    public void register(HideAndSeek plugin, SettingValueResolver resolver, SettingIconHelper iconHelper) {
        plugin.getSettingRegistry().register(
                SettingDefinition.builder(key, SettingType.FLOAT, Float.class)
                        .defaultValue(resolver.get(plugin, key, fallback))
                        .rangeFloat(min, max)
                        .description(description)
                        .customIcon(icon)
                        .build()
        );
    }
}

