package edu.uni.airportsim.logging;

import java.time.Instant;
import java.util.Objects;

public record LogEntry(Instant timestamp, LogLevel level, String category, String message) {
    public LogEntry {
        timestamp = Objects.requireNonNull(timestamp, "timestamp");
        level = Objects.requireNonNull(level, "level");
        category = category == null ? "GENERAL" : category;
        message = message == null ? "" : message;
    }

    public String format() {
        return timestamp + "|" + level + "|" + category + "|" + message;
    }

    public static LogEntry parse(String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid log line: " + line);
        }
        return new LogEntry(Instant.parse(parts[0]), LogLevel.valueOf(parts[1]), parts[2], parts[3]);
    }
}
