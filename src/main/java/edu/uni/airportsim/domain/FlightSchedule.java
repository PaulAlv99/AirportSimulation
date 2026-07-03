package edu.uni.airportsim.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record FlightSchedule(LocalDateTime departureTime, LocalDateTime arrivalTime) {
    public FlightSchedule {
        departureTime = Objects.requireNonNull(departureTime, "departureTime");
        arrivalTime = Objects.requireNonNull(arrivalTime, "arrivalTime");
    }
}
