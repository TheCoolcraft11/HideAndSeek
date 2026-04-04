package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

import java.util.List;

public final class LoadoutSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.hider-max-items", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "loadout.hider-max-items", 3))
                        .range(0, 7)
                        .description("Maximum number of items hiders can select in their loadout")
                        .customIcon(Material.CHEST)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.seeker-max-items", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "loadout.seeker-max-items", 4))
                        .range(0, 7)
                        .description("Maximum number of items seekers can select in their loadout")
                        .customIcon(Material.CHEST)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.hider-max-tokens", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "loadout.hider-max-tokens", 12))
                        .range(1, 50)
                        .description("Maximum tokens hiders can spend on items")
                        .customIcon(Material.GOLD_BLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.seeker-max-tokens", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "loadout.seeker-max-tokens", 12))
                        .range(1, 50)
                        .description("Maximum tokens seekers can spend on items")
                        .customIcon(Material.GOLD_BLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-common", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "loadout.token-cost-common", 1))
                        .range(1, 20)
                        .description("Token cost for Common rarity items")
                        .customIcon(Material.STONE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-uncommon", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "loadout.token-cost-uncommon", 2))
                        .range(1, 20)
                        .description("Token cost for Uncommon rarity items")
                        .customIcon(Material.IRON_INGOT)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-rare", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "loadout.token-cost-rare", 4))
                        .range(1, 20)
                        .description("Token cost for Rare rarity items")
                        .customIcon(Material.DIAMOND)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-epic", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "loadout.token-cost-epic", 6))
                        .range(1, 20)
                        .description("Token cost for Epic rarity items")
                        .customIcon(Material.AMETHYST_CLUSTER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-legendary", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "loadout.token-cost-legendary", 10))
                        .range(1, 30)
                        .description("Token cost for Legendary rarity items")
                        .customIcon(Material.NETHERITE_INGOT)
                        .build())
        );
    }
}
