package de.thecoolcraft11.hideAndSeek.setting;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;

public final class SettingValueResolver {

    @SuppressWarnings("unchecked")
    public <T> T get(HideAndSeek plugin, String path, T fallback) {
        Object value = plugin.getConfig().get("settings." + path);
        if (value == null) {
            return fallback;
        }

        try {
            if (fallback instanceof Float) {
                if (value instanceof Number) {
                    return (T) Float.valueOf(((Number) value).floatValue());
                }
                if (value instanceof String) {
                    return (T) Float.valueOf(Float.parseFloat((String) value));
                }
            }
            if (fallback instanceof Double) {
                if (value instanceof Number) {
                    return (T) Double.valueOf(((Number) value).doubleValue());
                }
                if (value instanceof String) {
                    return (T) Double.valueOf(Double.parseDouble((String) value));
                }
            }
            if (fallback instanceof Integer) {
                if (value instanceof Number) {
                    return (T) Integer.valueOf(((Number) value).intValue());
                }
                if (value instanceof String) {
                    return (T) Integer.valueOf(Integer.parseInt((String) value));
                }
            }
            if (fallback instanceof Long) {
                if (value instanceof Number) {
                    return (T) Long.valueOf(((Number) value).longValue());
                }
                if (value instanceof String) {
                    return (T) Long.valueOf(Long.parseLong((String) value));
                }
            }
            if (fallback instanceof Boolean) {
                if (value instanceof Boolean) {
                    return (T) value;
                }
                if (value instanceof String) {
                    return (T) Boolean.valueOf((String) value);
                }
            }
            if (fallback instanceof String) {
                return (T) value.toString();
            }

            return (T) value;
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid config value for " + path + ", using fallback: " + fallback + " (error: " + e.getMessage() + ")");
            return fallback;
        }
    }

    public <E extends Enum<E>> E getEnum(HideAndSeek plugin, String path, Class<E> enumClass, E fallback) {
        String value = plugin.getConfig().getString("settings." + path);
        if (value == null) {
            return fallback;
        }

        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid enum config value for " + path + ", using fallback: " + fallback);
            return fallback;
        }
    }
}

