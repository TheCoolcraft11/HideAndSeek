package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

public final class LoadoutSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new IntegerSettingSpec("loadout.hider-max-items", 3, 0, 7,
                        "Maximum number of items hiders can select in their loadout", Material.CHEST),
                new IntegerSettingSpec("loadout.seeker-max-items", 4, 0, 7,
                        "Maximum number of items seekers can select in their loadout", Material.CHEST),
                new IntegerSettingSpec("loadout.hider-max-tokens", 12, 1, 50,
                        "Maximum tokens hiders can spend on items", Material.GOLD_BLOCK),
                new IntegerSettingSpec("loadout.seeker-max-tokens", 12, 1, 50,
                        "Maximum tokens seekers can spend on items", Material.GOLD_BLOCK),
                new IntegerSettingSpec("loadout.token-cost-common", 1, 1, 20, "Token cost for Common rarity items",
                        Material.STONE),
                new IntegerSettingSpec("loadout.token-cost-uncommon", 2, 1, 20, "Token cost for Uncommon rarity items",
                        Material.IRON_INGOT),
                new IntegerSettingSpec("loadout.token-cost-rare", 4, 1, 20, "Token cost for Rare rarity items",
                        Material.DIAMOND),
                new IntegerSettingSpec("loadout.token-cost-epic", 6, 1, 20, "Token cost for Epic rarity items",
                        Material.AMETHYST_CLUSTER), new IntegerSettingSpec("loadout.token-cost-legendary", 10, 1, 30,
                        "Token cost for Legendary rarity items", Material.NETHERITE_INGOT));
    }
}
