package edu.uni.airportsim.runtime;

import java.time.LocalDateTime;

public record GateView(
        long id,
        String gateCode,
        String terminal,
        String state,
        boolean open,
        Long flightId,
        String flightNumber,
        int passengerQueue,
        int baggageQueue,
        LocalDateTime lastUpdated
) {
}
