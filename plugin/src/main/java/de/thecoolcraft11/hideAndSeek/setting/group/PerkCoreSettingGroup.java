package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.perk.PerkShopMode;
import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public final class PerkCoreSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                new IntegerSettingSpec(
                        "perks.perk.global.map-picker.view-height",
                        350,
                        -1000,
                        1000,
                        "MapPicker view height used by perk map selection screens",
                        Material.MAP
                ),
                new BooleanSettingSpec(
                        "perks.enabled",
                        true,
                        "Enable the perk system",
                        Material.NETHER_STAR,
                        true
                ),
                new EnumSettingSpec<>(
                        "perks.hider-shop-mode",
                        PerkShopMode.class,
                        PerkShopMode.INVENTORY,
                        "Shop mode for hiders",
                        Material.PLAYER_HEAD,
                        Map.of(
                                PerkShopMode.INVENTORY, Material.CHEST,
                                PerkShopMode.VENDING_MACHINE, Material.DROPPER
                        )
                ),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(
                        SettingDefinition.builder("perks.seeker-shop-mode", SettingType.ENUM, PerkShopMode.class)
                                .defaultValue(resolver.getEnum(
                                        plugin,
                                        "perks.seeker-shop-mode",
                                        PerkShopMode.class,
                                        resolver.getEnum(plugin, "perks.shop-mode", PerkShopMode.class, PerkShopMode.INVENTORY)
                                ))
                                .description("Shop mode for seekers")
                                .customIcon(Material.ENDER_EYE)
                                .valueIcons(Map.of(
                                        PerkShopMode.INVENTORY, Material.CHEST,
                                        PerkShopMode.VENDING_MACHINE, Material.DROPPER
                                ))
                                .build()
                ),
                new IntegerSettingSpec(
                        "perks.perks-per-round",
                        3,
                        1,
                        10,
                        "How many perks are selected each round",
                        Material.COMMAND_BLOCK
                ),
                new ListSettingSpec(
                        "perks.inventory-slots",
                        List.of(9, 10, 11, 12, 13, 14, 15, 16, 17),
                        "Inventory slots used for perk shop items",
                        Material.CHEST_MINECART
                ),
                new BooleanSettingSpec(
                        "perks.open-during-hiding",
                        false,
                        "Allow opening perk shop during hiding phase",
                        Material.ENDER_EYE,
                        true
                ),
                new IntegerSettingSpec(
                        "perks.hider-finite-player-limit",
                        1,
                        0,
                        20,
                        "Maximum number of hiders who can own the same hider perk",
                        Material.PLAYER_HEAD
                ),
                new IntegerSettingSpec(
                        "perks.seeker-finite-player-limit",
                        0,
                        0,
                        20,
                        "Maximum number of seekers who can own the same finite seeker perk",
                        Material.SKELETON_SKULL
                ),
                new IntegerSettingSpec(
                        "perks.finite-player-limit",
                        1,
                        0,
                        20,
                        "Default player limit for finite perks",
                        Material.BARRIER
                )
        );
    }
}


