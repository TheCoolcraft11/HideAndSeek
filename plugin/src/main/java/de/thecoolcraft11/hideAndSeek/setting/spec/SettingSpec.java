package de.thecoolcraft11.hideAndSeek.setting.spec;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.SettingIconHelper;
import de.thecoolcraft11.hideAndSeek.setting.SettingValueResolver;

public interface SettingSpec {
    void register(HideAndSeek plugin, SettingValueResolver resolver, SettingIconHelper iconHelper);
}

