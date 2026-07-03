package edu.uni.airportsim.persistence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonObject extends JsonValue {
    private final Map<String, JsonValue> values = new LinkedHashMap<>();

    public JsonObject put(String key, JsonValue value) {
        values.put(key, value == null ? JsonValue.nullValue() : value);
        return this;
    }

    public JsonValue get(String key) {
        return values.get(key);
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    public String getString(String key, String fallback) {
        JsonValue value = values.get(key);
        return value == null || value.isNull() ? fallback : value.asString();
    }

    public int getInt(String key, int fallback) {
        JsonValue value = values.get(key);
        return value == null || value.isNull() ? fallback : value.asNumber().intValue();
    }

    public JsonArray getArray(String key) {
        JsonValue value = values.get(key);
        return value == null || value.isNull() ? new JsonArray() : value.asArray();
    }

    public JsonObject getObject(String key) {
        JsonValue value = values.get(key);
        return value == null || value.isNull() ? new JsonObject() : value.asObject();
    }

    public Map<String, JsonValue> entries() {
        return Collections.unmodifiableMap(values);
    }
}
