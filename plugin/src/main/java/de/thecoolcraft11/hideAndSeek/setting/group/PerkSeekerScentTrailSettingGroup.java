package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import org.bukkit.Material;

import java.util.List;

public final class PerkSeekerScentTrailSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("perks.perk.seeker_scent_trail.enabled", true, "Enable Scent Trail",
                        Material.DIRT_PATH, true),
                new IntegerSettingSpec("perks.perk.seeker_scent_trail.cost", 90, 0, 10000, "Cost of Scent Trail",
                        Material.EMERALD),
                new LongSettingSpec("perks.perk.seeker_scent_trail.purchase-cooldown-ticks", 0L, 0L, 20000L,
                        "Purchase cooldown for Scent Trail", Material.CLOCK),
                new LongSettingSpec("perks.perk.seeker_scent_trail.trail-interval-ticks", 5L, 1L, 200L,
                        "Trail interval for Scent Trail", Material.DIRT_PATH),
                new DoubleSettingSpec("perks.perk.seeker_scent_trail.move-threshold", 0.3d, 0.0d, 10.0d,
                        "Movement threshold for Scent Trail", Material.DIRT_PATH),
                new IntegerSettingSpec("perks.perk.seeker_scent_trail.particle-lifetime-seconds", 8, 1, 120,
                        "Particle lifetime for Scent Trail", Material.CLOCK),
                new BooleanSettingSpec("perks.perk.seeker_scent_trail.finite", true, "Whether Scent Trail is finite",
                        Material.BARRIER, true));
    }
}
