package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import org.bukkit.Material;

import java.util.List;

public final class PerkHiderSeekerWarningSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("perks.perk.hider_seeker_warning.enabled", true, "Enable Seeker Warning",
                        Material.CLOCK, true),
                new IntegerSettingSpec("perks.perk.hider_seeker_warning.cost", 140, 0, 10000, "Cost of Seeker Warning",
                        Material.EMERALD),
                new LongSettingSpec("perks.perk.hider_seeker_warning.cooldown-ticks", 0L, 0L, 20000L,
                        "Purchase cooldown for Seeker Warning", Material.CLOCK),
                new DoubleSettingSpec("perks.perk.hider_seeker_warning.range", 12.0d, 0.0d, 100.0d,
                        "Detection range for Seeker Warning", Material.ENDER_PEARL),
                new DoubleSettingSpec("perks.perk.hider_seeker_warning.fov", 45.0d, 0.0d, 180.0d,
                        "FOV for Seeker Warning", Material.COMPASS),
                new DoubleSettingSpec("perks.perk.hider_seeker_warning.movement-range", 8.0d, 0.0d, 100.0d,
                        "Movement range for Seeker Warning", Material.ENDER_EYE),
                new LongSettingSpec("perks.perk.hider_seeker_warning.trigger-cooldown-ticks", 60L, 0L, 20000L,
                        "Trigger cooldown for Seeker Warning", Material.CLOCK));
    }
}
