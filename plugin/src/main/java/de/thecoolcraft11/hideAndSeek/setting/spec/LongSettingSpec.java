package de.thecoolcraft11.hideAndSeek.setting.spec;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.SettingIconHelper;
import de.thecoolcraft11.hideAndSeek.setting.SettingValueResolver;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

public final class LongSettingSpec implements SettingSpec {

    private final String key;
    private final long fallback;
    private final long min;
    private final long max;
    private final String description;
    private final Material icon;

    public LongSettingSpec(String key, long fallback, long min, long max, String description, Material icon) {
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
                SettingDefinition.builder(key, SettingType.LONG, Long.class)
                        .defaultValue(resolver.get(plugin, key, fallback))
                        .rangeLong(min, max)
                        .description(description)
                        .customIcon(icon)
                        .build()
        );
    }
}

