package edu.uni.airportsim.runtime;

import java.time.LocalDateTime;

public record EventView(
        LocalDateTime occurredAt,
        String level,
        String category,
        String message
) {
}
