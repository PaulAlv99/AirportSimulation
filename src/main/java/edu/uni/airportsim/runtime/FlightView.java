package edu.uni.airportsim.runtime;

import java.time.LocalDateTime;

public record FlightView(
        long id,
        String flightNumber,
        String airline,
        String originLabel,
        String destinationLabel,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        String status,
        int delayMinutes,
        String gate,
        String runway,
        String weatherNotes
) {
}
