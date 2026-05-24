package de.thecoolcraft11.hideAndSeek.setting.spec;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.SettingIconHelper;
import de.thecoolcraft11.hideAndSeek.setting.SettingValueResolver;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.function.BiFunction;

public final class IntegerSettingSpec implements SettingSpec {

    private final String key;

    private final int fallback;

    private final int min;

    private final int max;

    private final String description;

    private final Material icon;

    private final BiFunction<HideAndSeek, Object, ItemStack> itemProvider;

    private final ItemStack iconStack;

    public IntegerSettingSpec(String key, int fallback, int min, int max, String description, Material icon) {
        this(key, fallback, min, max, description, icon, null, null);
    }

    public IntegerSettingSpec(String key, int fallback, int min, int max, String description, ItemStack iconStack) {
        this(key, fallback, min, max, description, null, null, iconStack);
    }

    public IntegerSettingSpec(String key, int fallback, int min, int max, String description, Material icon, BiFunction<HideAndSeek, Object, ItemStack> itemProvider) {
        this(key, fallback, min, max, description, icon, itemProvider, null);
    }

    public IntegerSettingSpec(String key, int fallback, int min, int max, String description, Material icon, BiFunction<HideAndSeek, Object, ItemStack> itemProvider, ItemStack iconStack) {
        this.key = key;
        this.fallback = fallback;
        this.min = min;
        this.max = max;
        this.description = description;
        this.icon = icon;
        this.itemProvider = itemProvider;
        this.iconStack = iconStack;
    }

    @Override
    public void register(HideAndSeek plugin, SettingValueResolver resolver, SettingIconHelper iconHelper) {
        plugin.getConfigRegistry().register("settings." + key, Integer.class, fallback);
        SettingDefinition.Builder<Integer> builder;
        if (iconStack != null) {
            builder = SettingDefinition.builder(key, SettingType.INTEGER, Integer.class).defaultValue(
                    resolver.get(plugin, key, fallback)).range(
                    min, max).description(description).customIcon(iconStack);
        } else {
            builder = SettingDefinition.builder(key, SettingType.INTEGER, Integer.class).defaultValue(
                    resolver.get(plugin, key, fallback)).range(
                    min, max).description(description).customIcon(icon);
        }
        if (itemProvider != null) {
            builder.itemProvider(value -> itemProvider.apply(plugin, value));
        }
        plugin.getSettingRegistry().register(builder.build());
    }
}
