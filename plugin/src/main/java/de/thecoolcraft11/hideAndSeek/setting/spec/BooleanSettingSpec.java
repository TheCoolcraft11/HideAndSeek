package de.thecoolcraft11.hideAndSeek.setting.spec;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.SettingIconHelper;
import de.thecoolcraft11.hideAndSeek.setting.SettingValueResolver;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;
import java.util.Map;

public final class BooleanSettingSpec implements SettingSpec {

    private final String key;

    private final boolean fallback;

    private final String description;

    private final Material icon;

    private final boolean useValueIcons;

    public BooleanSettingSpec(String key, boolean fallback, String description, Material icon, boolean useValueIcons) {
        this.key = key;
        this.fallback = fallback;
        this.description = description;
        this.icon = icon;
        this.useValueIcons = useValueIcons;
    }

    @Override
    public void register(HideAndSeek plugin, SettingValueResolver resolver, SettingIconHelper iconHelper) {
        var builder = SettingDefinition.builder(key, SettingType.BOOLEAN, Boolean.class).defaultValue(
                fallback).description(description).customIcon(icon);
        if (useValueIcons) {
            builder.valueIconStacks(Map.of(Boolean.TRUE, iconHelper.enchanted(icon, true), Boolean.FALSE,
                    iconHelper.enchanted(icon, false)));
        }
        plugin.getConfigRegistry().register("settings." + key, Boolean.class, fallback);
        plugin.getSettingRegistry().register(builder.build());
    }
}
