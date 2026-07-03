package edu.uni.airportsim.simulation;

import java.time.LocalDateTime;
import java.util.Objects;

public record SimulationEvent(LocalDateTime occurredAt, String category, String message) {
    public SimulationEvent {
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        category = category == null ? "GENERAL" : category;
        message = message == null ? "" : message;
    }
}
