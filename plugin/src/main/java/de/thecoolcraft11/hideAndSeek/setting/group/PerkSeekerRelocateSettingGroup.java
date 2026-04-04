package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import org.bukkit.Material;

import java.util.List;

public final class PerkSeekerRelocateSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                new BooleanSettingSpec(
                        "perks.perk.seeker_relocate.enabled",
                        true,
                        "Enable Relocate",
                        Material.LEAD,
                        true
                ),
                new IntegerSettingSpec(
                        "perks.perk.seeker_relocate.cost",
                        280,
                        0,
                        10000,
                        "Cost of Relocate",
                        Material.EMERALD
                ),
                new LongSettingSpec(
                        "perks.perk.seeker_relocate.purchase-cooldown-ticks",
                        1200L,
                        0L,
                        20000L,
                        "Purchase cooldown for Relocate",
                        Material.CLOCK
                ),
                new IntegerSettingSpec(
                        "perks.perk.seeker_relocate.escape-seconds",
                        60,
                        1,
                        1000,
                        "Escape time for Relocate",
                        Material.CLOCK
                ),
                new DoubleSettingSpec(
                        "perks.perk.seeker_relocate.radius",
                        6.0d,
                        0.0d,
                        200.0d,
                        "Relocate radius",
                        Material.SLIME_BALL
                ),
                new BooleanSettingSpec(
                        "perks.perk.seeker_relocate.finite",
                        false,
                        "Whether Relocate is finite",
                        Material.BARRIER,
                        true
                )
        );
    }
}

