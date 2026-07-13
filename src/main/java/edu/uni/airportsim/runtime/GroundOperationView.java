package edu.uni.airportsim.runtime;

import java.time.LocalDateTime;

public record GroundOperationView(
        long id,
        Long flightId,
        String flightNumber,
        String gateCode,
        String operationType,
        String status,
        LocalDateTime startedAt,
        LocalDateTime dueAt,
        LocalDateTime completedAt,
        int delayMinutes
) {
}
