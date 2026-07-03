package edu.uni.airportsim.weather;

import java.time.LocalDateTime;

public record WeatherSnapshot(
        String airportId,
        LocalDateTime observedAt,
        Temperature temperature,
        Wind wind,
        Precipitation precipitation,
        Visibility visibility,
        CloudCondition cloudCondition,
        RunwaySurfaceCondition runwaySurfaceCondition,
        WeatherSeverity severity
) {
    public WeatherSnapshot {
        if (airportId == null || airportId.isBlank()) {
            throw new IllegalArgumentException("airportId must not be blank");
        }
        observedAt = observedAt == null ? LocalDateTime.now() : observedAt;
        temperature = temperature == null ? Temperature.of(20) : temperature;
        wind = wind == null ? new Wind(0, 0, 0) : wind;
        precipitation = precipitation == null ? new Precipitation(0, 0, false, false) : precipitation;
        visibility = visibility == null ? new Visibility(10_000, false) : visibility;
        cloudCondition = cloudCondition == null ? new CloudCondition(0, 10_000, "Clear") : cloudCondition;
        runwaySurfaceCondition = runwaySurfaceCondition == null ? RunwaySurfaceCondition.DRY : runwaySurfaceCondition;
        severity = severity == null ? WeatherSeverity.NORMAL : severity;
    }

    public WeatherSnapshot withSeverity(WeatherSeverity newSeverity) {
        return new WeatherSnapshot(
                airportId,
                observedAt,
                temperature,
                wind,
                precipitation,
                visibility,
                cloudCondition,
                runwaySurfaceCondition,
                newSeverity
        );
    }
}
