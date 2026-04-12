package dev.noah.perplayerkit.gui.configurable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class GuiContext {
    private static final GuiContext EMPTY = new GuiContext(Collections.emptyMap());

    private final Map<String, Object> values;

    private GuiContext(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    public static GuiContext empty() {
        return EMPTY;
    }

    public GuiContext with(String key, Object value) {
        if (key == null || value == null) {
            return this;
        }

        Map<String, Object> updated = new LinkedHashMap<>(values);
        updated.put(key, value);
        return new GuiContext(updated);
    }

    public GuiContext withAll(Map<String, Object> additionalValues) {
        if (additionalValues == null || additionalValues.isEmpty()) {
            return this;
        }

        Map<String, Object> updated = new LinkedHashMap<>(values);
        additionalValues.forEach((key, value) -> {
            if (key != null && value != null) {
                updated.put(key, value);
            }
        });
        return new GuiContext(updated);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public String getString(String key) {
        Object value = values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public Integer getInt(String key) {
        Object value = values.get(key);
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public UUID getUuid(String key) {
        Object value = values.get(key);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String stringValue) {
            try {
                return UUID.fromString(stringValue);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    public Map<String, Object> values() {
        return values;
    }
}
