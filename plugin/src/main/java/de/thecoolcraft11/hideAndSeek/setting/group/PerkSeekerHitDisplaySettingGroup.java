package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.BooleanSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

@SuppressWarnings("unused")
public final class PerkSeekerHitDisplaySettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("perks.perk.seeker_hit_display.enabled", true, "Enable Hit Display",
                        Material.FILLED_MAP, true),
                new IntegerSettingSpec("perks.perk.seeker_hit_display.cost", 120, 0, 10000, "Cost of Hit Display",
                        Material.EMERALD),
                new BooleanSettingSpec("perks.perk.seeker_hit_display.finite", true, "Whether Hit Display is finite",
                        Material.BARRIER, true));
    }
}


