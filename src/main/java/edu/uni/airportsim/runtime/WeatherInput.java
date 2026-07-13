package edu.uni.airportsim.runtime;

public record WeatherInput(
        double temperatureCelsius,
        double feelsLikeCelsius,
        double windSpeedKmh,
        double windGustKmh,
        int windDirectionDegrees,
        double rainMmPerHour,
        double snowMmPerHour,
        boolean hail,
        boolean thunderstorm,
        int visibilityMeters,
        boolean fog,
        int cloudCoveragePercent,
        int ceilingMeters,
        String cloudLabel,
        String runwaySurface,
        String severityCode
) {
}
