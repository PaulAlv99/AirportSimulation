package edu.uni.airportsim.runtime;

import java.time.LocalDateTime;

public record WeatherView(
        String airportId,
        LocalDateTime observedAt,
        double temperatureCelsius,
        double feelsLikeCelsius,
        double windSpeedKmh,
        double windGustKmh,
        int windDirectionDegrees,
        int visibilityMeters,
        String cloudLabel,
        String runwaySurface,
        String severityCode,
        String severityLabel,
        String message
) {
}
