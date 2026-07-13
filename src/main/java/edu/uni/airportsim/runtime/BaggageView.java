package edu.uni.airportsim.runtime;

import java.time.LocalDateTime;

public record BaggageView(
        long id,
        String tag,
        long flightId,
        String flightNumber,
        String status,
        String belt,
        String exceptionReason,
        LocalDateTime lastUpdated
) {
}
