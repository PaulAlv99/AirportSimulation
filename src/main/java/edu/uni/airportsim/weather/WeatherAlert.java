package edu.uni.airportsim.weather;

import java.time.LocalDateTime;

public record WeatherAlert(
        LocalDateTime timestamp,
        WeatherSeverity severity,
        String message,
        String airportId,
        String runwayId,
        String flightId
) {
    public WeatherAlert {
        timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        severity = severity == null ? WeatherSeverity.NORMAL : severity;
        message = message == null ? "" : message;
        airportId = airportId == null ? "" : airportId;
        runwayId = runwayId == null ? "" : runwayId;
        flightId = flightId == null ? "" : flightId;
    }
}
