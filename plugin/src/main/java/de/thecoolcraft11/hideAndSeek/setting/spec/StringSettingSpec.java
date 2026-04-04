package de.thecoolcraft11.hideAndSeek.setting.spec;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.SettingIconHelper;
import de.thecoolcraft11.hideAndSeek.setting.SettingValueResolver;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiFunction;

public final class StringSettingSpec implements SettingSpec {

    private final String key;
    private final String fallback;
    private final String description;
    private final Material icon;
    private final BiFunction<HideAndSeek, Object, ItemStack> itemProvider;

    public StringSettingSpec(String key, String fallback, String description, Material icon, BiFunction<HideAndSeek, Object, ItemStack> itemProvider) {
        this.key = key;
        this.fallback = fallback;
        this.description = description;
        this.icon = icon;
        this.itemProvider = itemProvider;
    }

    @Override
    public void register(HideAndSeek plugin, SettingValueResolver resolver, SettingIconHelper iconHelper) {
        var builder = SettingDefinition.builder(key, SettingType.STRING, String.class)
                .defaultValue(resolver.get(plugin, key, fallback))
                .description(description)
                .customIcon(icon);

        if (itemProvider != null) {
            builder.itemProvider(value -> itemProvider.apply(plugin, value));
        }

        plugin.getSettingRegistry().register(builder.build());
    }
}


