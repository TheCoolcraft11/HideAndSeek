package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import org.bukkit.Material;

import java.util.List;

public final class PerkSeekerElytraRushSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                new BooleanSettingSpec(
                        "perks.perk.seeker_elytra_rush.enabled",
                        true,
                        "Enable Elytra Rush",
                        Material.FEATHER,
                        true
                ),
                new IntegerSettingSpec(
                        "perks.perk.seeker_elytra_rush.cost",
                        180,
                        0,
                        10000,
                        "Cost of Elytra Rush",
                        Material.EMERALD
                ),
                new LongSettingSpec(
                        "perks.perk.seeker_elytra_rush.purchase-cooldown-ticks",
                        600L,
                        0L,
                        20000L,
                        "Purchase cooldown for Elytra Rush",
                        Material.CLOCK
                ),
                new LongSettingSpec(
                        "perks.perk.seeker_elytra_rush.duration-ticks",
                        600L,
                        1L,
                        20000L,
                        "Glide duration for Elytra Rush",
                        Material.FEATHER
                ),
                new DoubleSettingSpec(
                        "perks.perk.seeker_elytra_rush.launch-power",
                        0.8d,
                        0.0d,
                        5.0d,
                        "Launch power for Elytra Rush",
                        Material.FEATHER
                ),
                new BooleanSettingSpec(
                        "perks.perk.seeker_elytra_rush.finite",
                        false,
                        "Whether Elytra Rush is finite",
                        Material.BARRIER,
                        true
                )
        );
    }
}

