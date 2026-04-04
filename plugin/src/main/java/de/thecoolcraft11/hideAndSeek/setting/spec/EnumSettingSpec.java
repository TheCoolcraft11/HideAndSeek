package de.thecoolcraft11.hideAndSeek.setting.spec;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.SettingIconHelper;
import de.thecoolcraft11.hideAndSeek.setting.SettingValueResolver;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

import java.util.Map;

public final class EnumSettingSpec<E extends Enum<E>> implements SettingSpec {

    private final String key;
    private final Class<E> enumClass;
    private final E fallback;
    private final String description;
    private final Material icon;
    private final Map<E, Material> valueIcons;

    public EnumSettingSpec(String key, Class<E> enumClass, E fallback, String description, Material icon, Map<E, Material> valueIcons) {
        this.key = key;
        this.enumClass = enumClass;
        this.fallback = fallback;
        this.description = description;
        this.icon = icon;
        this.valueIcons = valueIcons;
    }

    @Override
    public void register(HideAndSeek plugin, SettingValueResolver resolver, SettingIconHelper iconHelper) {
        plugin.getSettingRegistry().register(
                SettingDefinition.builder(key, SettingType.ENUM, enumClass)
                        .defaultValue(resolver.getEnum(plugin, key, enumClass, fallback))
                        .description(description)
                        .customIcon(icon)
                        .valueIcons(valueIcons)
                        .build()
        );
    }
}

