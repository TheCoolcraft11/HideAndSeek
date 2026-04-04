package de.thecoolcraft11.hideAndSeek.setting.spec;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.SettingIconHelper;
import de.thecoolcraft11.hideAndSeek.setting.SettingValueResolver;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

import java.util.List;

public final class ListSettingSpec implements SettingSpec {

    private final String key;
    private final List<?> fallback;
    private final String description;
    private final Material icon;

    public ListSettingSpec(String key, List<?> fallback, String description, Material icon) {
        this.key = key;
        this.fallback = fallback;
        this.description = description;
        this.icon = icon;
    }

    @Override
    public void register(HideAndSeek plugin, SettingValueResolver resolver, SettingIconHelper iconHelper) {
        plugin.getSettingRegistry().register(
                SettingDefinition.builder(key, SettingType.LIST, List.class)
                        .defaultValue(resolver.get(plugin, key, fallback))
                        .description(description)
                        .customIcon(icon)
                        .build()
        );
    }
}

