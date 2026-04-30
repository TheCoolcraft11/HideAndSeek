package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.BooleanSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

public final class PerkSeekerAutoAimSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("perks.perk.seeker_auto_aim.enabled", true, "Enable Auto Aim",
                        Material.TARGET, true),
                new IntegerSettingSpec("perks.perk.seeker_auto_aim.cost", 180, 0, 10000, "Cost of Auto Aim",
                        Material.EMERALD),
                new BooleanSettingSpec("perks.perk.seeker_auto_aim.finite", true, "Whether Auto Aim is finite",
                        Material.BARRIER, true));
    }
}

