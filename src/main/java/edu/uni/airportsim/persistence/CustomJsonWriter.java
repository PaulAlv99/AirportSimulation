package edu.uni.airportsim.persistence;

import java.util.Map;

public class CustomJsonWriter {
    public String write(JsonValue value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        if (value instanceof JsonObject object) {
            return writeObject(object);
        }
        if (value instanceof JsonArray array) {
            return writeArray(array);
        }
        if (value instanceof JsonString string) {
            return quote(string.value());
        }
        if (value instanceof JsonNumber number) {
            return number.value().toPlainString();
        }
        if (value instanceof JsonBoolean bool) {
            return Boolean.toString(bool.value());
        }
        throw new IllegalArgumentException("Unsupported JSON value type: " + value.getClass());
    }

    private String writeObject(JsonObject object) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, JsonValue> entry : object.entries().entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append(quote(entry.getKey())).append(':').append(write(entry.getValue()));
            first = false;
        }
        return builder.append('}').toString();
    }

    private String writeArray(JsonArray array) {
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (JsonValue value : array.values()) {
            if (!first) {
                builder.append(',');
            }
            builder.append(write(value));
            first = false;
        }
        return builder.append(']').toString();
    }

    private String quote(String value) {
        StringBuilder builder = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(character);
            }
        }
        return builder.append('"').toString();
    }
}
