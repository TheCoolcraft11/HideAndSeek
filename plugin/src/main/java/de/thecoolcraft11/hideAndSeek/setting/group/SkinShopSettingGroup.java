package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

import java.util.List;

public final class SkinShopSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("skin-shop.points-per-coin", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "skin-shop.points-per-coin", 25))
                        .range(1, 500)
                        .description("How many points are converted into 1 coin at round end")
                        .customIcon(Material.SUNFLOWER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("skin-shop.cost-common", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "skin-shop.cost-common", 100))
                        .range(0, 100000)
                        .description("Coin cost for Common skin variants")
                        .customIcon(Material.IRON_NUGGET)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("skin-shop.cost-uncommon", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "skin-shop.cost-uncommon", 250))
                        .range(0, 100000)
                        .description("Coin cost for Uncommon skin variants")
                        .customIcon(Material.IRON_INGOT)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("skin-shop.cost-rare", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "skin-shop.cost-rare", 500))
                        .range(0, 100000)
                        .description("Coin cost for Rare skin variants")
                        .customIcon(Material.DIAMOND)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("skin-shop.cost-epic", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "skin-shop.cost-epic", 900))
                        .range(0, 100000)
                        .description("Coin cost for Epic skin variants")
                        .customIcon(Material.AMETHYST_CLUSTER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("skin-shop.cost-legendary", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "skin-shop.cost-legendary", 1500))
                        .range(0, 100000)
                        .description("Coin cost for Legendary skin variants")
                        .customIcon(Material.NETHERITE_INGOT)
                        .build())
        );
    }
}
