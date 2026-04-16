package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import org.bukkit.Material;

import java.util.List;

public final class PerkHiderDoubleJumpSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("perks.perk.hider_double_jump.enabled", true, "Enable Double Jump",
                        Material.FEATHER, true),
                new IntegerSettingSpec("perks.perk.hider_double_jump.cost", 120, 0, 10000, "Cost of Double Jump",
                        Material.EMERALD),
                new LongSettingSpec("perks.perk.hider_double_jump.cooldown-ticks", 0L, 0L, 20000L,
                        "Purchase cooldown for Double Jump", Material.CLOCK),
                new DoubleSettingSpec("perks.perk.hider_double_jump.jump-power", 0.7d, 0.0d, 3.0d,
                        "Vertical boost for Double Jump", Material.FEATHER),
                new DoubleSettingSpec("perks.perk.hider_double_jump.horizontal-boost", 0.1d, 0.0d, 3.0d,
                        "Horizontal boost for Double Jump", Material.FEATHER));
    }
}
