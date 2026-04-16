package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import org.bukkit.Material;

import java.util.List;

public final class PerkSeekerProximityMeterSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                new BooleanSettingSpec("perks.perk.seeker_proximity_meter.enabled", true, "Enable Proximity Meter",
                        Material.CLOCK, true),
                new IntegerSettingSpec("perks.perk.seeker_proximity_meter.cost", 100, 0, 10000,
                        "Cost of Proximity Meter", Material.EMERALD),
                new LongSettingSpec("perks.perk.seeker_proximity_meter.purchase-cooldown-ticks", 0L, 0L, 20000L,
                        "Purchase cooldown for Proximity Meter", Material.CLOCK),
                new DoubleSettingSpec("perks.perk.seeker_proximity_meter.threshold-burning", 5.0d, 0.0d, 200.0d,
                        "Burning threshold for Proximity Meter", Material.REDSTONE),
                new DoubleSettingSpec("perks.perk.seeker_proximity_meter.threshold-very-warm", 10.0d, 0.0d, 200.0d,
                        "Very warm threshold for Proximity Meter", Material.REDSTONE),
                new DoubleSettingSpec("perks.perk.seeker_proximity_meter.threshold-warm", 18.0d, 0.0d, 200.0d,
                        "Warm threshold for Proximity Meter", Material.REDSTONE),
                new DoubleSettingSpec("perks.perk.seeker_proximity_meter.threshold-lukewarm", 30.0d, 0.0d, 200.0d,
                        "Lukewarm threshold for Proximity Meter", Material.REDSTONE),
                new DoubleSettingSpec("perks.perk.seeker_proximity_meter.threshold-cool", 50.0d, 0.0d, 200.0d,
                        "Cool threshold for Proximity Meter", Material.REDSTONE),
                new BooleanSettingSpec("perks.perk.seeker_proximity_meter.finite", true,
                        "Whether Proximity Meter is finite", Material.BARRIER, true));
    }
}
