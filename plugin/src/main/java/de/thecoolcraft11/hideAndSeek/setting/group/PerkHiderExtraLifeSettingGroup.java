package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.BooleanSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.LongSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

public final class PerkHiderExtraLifeSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("perks.perk.hider_extra_life.enabled", true, "Enable Extra Life",
                        Material.TOTEM_OF_UNDYING, true),
                new IntegerSettingSpec("perks.perk.hider_extra_life.cost", 200, 0, 10000, "Cost of Extra Life",
                        Material.EMERALD),
                new LongSettingSpec("perks.perk.hider_extra_life.cooldown-ticks", 0L, 0L, 20000L,
                        "Purchase cooldown for Extra Life", Material.CLOCK),
                new IntegerSettingSpec("perks.perk.hider_extra_life.points-per-heart", 100, 1, 10000,
                        "Points required per absorption heart", Material.GOLD_NUGGET),
                new IntegerSettingSpec("perks.perk.hider_extra_life.max-hearts", 5, 1, 20, "Maximum absorption hearts",
                        Material.GOLDEN_APPLE));
    }
}
