package edu.uni.airportsim.persistence;

import java.math.BigDecimal;

public abstract class JsonValue {
    public JsonObject asObject() {
        if (this instanceof JsonObject object) {
            return object;
        }
        throw new IllegalStateException("JSON value is not an object");
    }

    public JsonArray asArray() {
        if (this instanceof JsonArray array) {
            return array;
        }
        throw new IllegalStateException("JSON value is not an array");
    }

    public String asString() {
        if (this instanceof JsonString string) {
            return string.value();
        }
        throw new IllegalStateException("JSON value is not a string");
    }

    public BigDecimal asNumber() {
        if (this instanceof JsonNumber number) {
            return number.value();
        }
        throw new IllegalStateException("JSON value is not a number");
    }

    public boolean asBoolean() {
        if (this instanceof JsonBoolean bool) {
            return bool.value();
        }
        throw new IllegalStateException("JSON value is not a boolean");
    }

    public boolean isNull() {
        return this instanceof JsonNull;
    }

    public static JsonValue of(String value) {
        return value == null ? JsonNull.INSTANCE : new JsonString(value);
    }

    public static JsonValue of(Number value) {
        return value == null ? JsonNull.INSTANCE : new JsonNumber(new BigDecimal(value.toString()));
    }

    public static JsonValue of(boolean value) {
        return new JsonBoolean(value);
    }

    public static JsonValue nullValue() {
        return JsonNull.INSTANCE;
    }
}

final class JsonString extends JsonValue {
    private final String value;

    JsonString(String value) {
        this.value = value;
    }

    String value() {
        return value;
    }
}

final class JsonNumber extends JsonValue {
    private final BigDecimal value;

    JsonNumber(BigDecimal value) {
        this.value = value;
    }

    BigDecimal value() {
        return value;
    }
}

final class JsonBoolean extends JsonValue {
    private final boolean value;

    JsonBoolean(boolean value) {
        this.value = value;
    }

    boolean value() {
        return value;
    }
}

final class JsonNull extends JsonValue {
    static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {
    }
}
