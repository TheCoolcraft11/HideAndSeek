package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;

import java.util.List;

public final class TimerSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        HideAndSeek plugin = (HideAndSeek) HideAndSeek.getActiveInstance();
        if (plugin.getTimerPlugin() == null) {
            return List.of();
        }
        return TimerSettingsLoader.load();
    }
}
