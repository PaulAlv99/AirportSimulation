package edu.uni.airportsim.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonArray extends JsonValue {
    private final List<JsonValue> values = new ArrayList<>();

    public JsonArray add(JsonValue value) {
        values.add(value == null ? JsonValue.nullValue() : value);
        return this;
    }

    public JsonValue get(int index) {
        return values.get(index);
    }

    public int size() {
        return values.size();
    }

    public List<JsonValue> values() {
        return Collections.unmodifiableList(values);
    }
}
