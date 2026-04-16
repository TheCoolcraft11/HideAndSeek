package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

public final class SkinShopSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new IntegerSettingSpec("skin-shop.points-per-coin", 25, 1, 500,
                        "How many points are converted into 1 coin at round end", Material.SUNFLOWER),
                new IntegerSettingSpec("skin-shop.cost-common", 100, 0, 100000, "Coin cost for Common skin variants",
                        Material.IRON_NUGGET), new IntegerSettingSpec("skin-shop.cost-uncommon", 250, 0, 100000,
                        "Coin cost for Uncommon skin variants", Material.IRON_INGOT),
                new IntegerSettingSpec("skin-shop.cost-rare", 500, 0, 100000, "Coin cost for Rare skin variants",
                        Material.DIAMOND),
                new IntegerSettingSpec("skin-shop.cost-epic", 900, 0, 100000, "Coin cost for Epic skin variants",
                        Material.AMETHYST_CLUSTER), new IntegerSettingSpec("skin-shop.cost-legendary", 1500, 0, 100000,
                        "Coin cost for Legendary skin variants", Material.NETHERITE_INGOT));
    }
}
