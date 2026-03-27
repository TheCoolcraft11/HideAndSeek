package de.thecoolcraft11.hideAndSeek.items.effects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KillEffectManager {

    private static final Map<String, KillEffect> KILL_EFFECTS = new ConcurrentHashMap<>();

    private KillEffectManager() {
    }

    public static void registerKillEffect(String killEffectId, KillEffect killEffect) {
        if (killEffectId == null || killEffectId.isBlank() || killEffect == null) {
            return;
        }
        KILL_EFFECTS.put(killEffectId, killEffect);
    }

    public static KillEffect getKillEffect(String killEffectId) {
        if (killEffectId == null || killEffectId.isBlank()) {
            return null;
        }
        return KILL_EFFECTS.get(killEffectId);
    }

    public static void clear() {
        KILL_EFFECTS.clear();
    }

}
