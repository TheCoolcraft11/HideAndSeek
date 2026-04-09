package de.thecoolcraft11.hideAndSeek.setting;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.group.SettingGroup;

import java.util.List;

public final class SettingGroupRegistrar {

    private SettingGroupRegistrar() {
    }

    public static void register(HideAndSeek plugin, List<SettingGroup> groups) {
        SettingValueResolver resolver = new SettingValueResolver();
        SettingIconHelper iconHelper = new SettingIconHelper();

        int registered = 0;

        for (SettingGroup group : groups) {
            for (var spec : group.settings()) {
                spec.register(plugin, resolver, iconHelper);
                registered++;
            }
        }
        plugin.getLogger().info("Registered " + registered + " settings");
    }
}

