package edu.uni.airportsim.runtime;

import java.time.LocalDateTime;

public record PassengerManifestView(
        long id,
        long flightId,
        String passengerCode,
        String fullName,
        String seatNumber,
        String travelDocument,
        String status,
        boolean checkedIn,
        boolean securityCleared,
        boolean boarded,
        boolean missedConnection,
        int baggageCount,
        LocalDateTime lastUpdated
) {
}
