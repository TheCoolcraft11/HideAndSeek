package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.BooleanSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.LongSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

public final class PerkSeekerDeathZoneSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("perks.perk.seeker_death_zone.enabled", true, "Enable Death Zone",
                        Material.WITHER_ROSE, true),
                new IntegerSettingSpec("perks.perk.seeker_death_zone.cost", 350, 0, 10000, "Cost of Death Zone",
                        Material.EMERALD),
                new LongSettingSpec("perks.perk.seeker_death_zone.purchase-cooldown-ticks", 3600L, 0L, 20000L,
                        "Purchase cooldown for Death Zone", Material.CLOCK),
                new IntegerSettingSpec("perks.perk.seeker_death_zone.escape-seconds", 60, 1, 1000,
                        "Escape time for Death Zone", Material.CLOCK),
                new IntegerSettingSpec("perks.perk.seeker_death_zone.radius", 32, 1, 256,
                        "Picker circle radius for Death Zone", Material.MAP),
                new BooleanSettingSpec("perks.perk.seeker_death_zone.finite", false, "Whether Death Zone is finite",
                        Material.BARRIER, true));
    }
}
