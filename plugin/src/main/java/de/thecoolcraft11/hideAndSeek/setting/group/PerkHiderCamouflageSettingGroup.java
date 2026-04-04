package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.BooleanSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.LongSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

public final class PerkHiderCamouflageSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                new BooleanSettingSpec(
                        "perks.perk.hider_camouflage.enabled",
                        true,
                        "Enable Camouflage",
                        Material.FERN,
                        true
                ),
                new IntegerSettingSpec(
                        "perks.perk.hider_camouflage.cost",
                        80,
                        0,
                        10000,
                        "Cost of Camouflage",
                        Material.EMERALD
                ),
                new LongSettingSpec(
                        "perks.perk.hider_camouflage.cooldown-ticks",
                        0L,
                        0L,
                        20000L,
                        "Purchase cooldown for Camouflage",
                        Material.CLOCK
                ),
                new LongSettingSpec(
                        "perks.perk.hider_camouflage.re-cleanse-interval-ticks",
                        100L,
                        1L,
                        20000L,
                        "Interval between Camouflage re-cleanses",
                        Material.POTION
                )
        );
    }
}

