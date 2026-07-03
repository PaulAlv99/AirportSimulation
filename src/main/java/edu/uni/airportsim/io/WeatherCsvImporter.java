package edu.uni.airportsim.io;

import edu.uni.airportsim.weather.CloudCondition;
import edu.uni.airportsim.weather.Precipitation;
import edu.uni.airportsim.weather.RunwaySurfaceCondition;
import edu.uni.airportsim.weather.Temperature;
import edu.uni.airportsim.weather.Visibility;
import edu.uni.airportsim.weather.WeatherSeverity;
import edu.uni.airportsim.weather.WeatherSnapshot;
import edu.uni.airportsim.weather.Wind;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class WeatherCsvImporter implements CsvImporter<WeatherSnapshot> {
    private final CsvParser parser;

    public WeatherCsvImporter(CsvParser parser) {
        this.parser = parser;
    }

    @Override
    public List<WeatherSnapshot> importFrom(Path csvFile) {
        return parser.parse(csvFile)
                .stream()
                .map(this::toWeather)
                .toList();
    }

    private WeatherSnapshot toWeather(Map<String, String> row) {
        String airportId = value(row, "airportId", value(row, "airport_id", "APT-LIS"));
        LocalDateTime observedAt = LocalDateTime.parse(value(row, "observedAt", value(row, "observed_at", "2026-01-01T08:00:00")));
        double temperatureCelsius = doubleValue(row, "temperatureCelsius", doubleValue(row, "temperature_celsius", 18));
        double feelsLikeCelsius = doubleValue(row, "feelsLikeCelsius", doubleValue(row, "feels_like_celsius", temperatureCelsius));
        double windSpeedKmh = doubleValue(row, "windSpeedKmh", doubleValue(row, "wind_speed_kmh", 12));
        double windGustKmh = doubleValue(row, "windGustKmh", doubleValue(row, "wind_gust_kmh", windSpeedKmh));
        int windDirectionDegrees = intValue(row, "windDirectionDegrees", intValue(row, "wind_direction_degrees", 270));
        double rainMmPerHour = doubleValue(row, "rainMmPerHour", doubleValue(row, "rain_mm_per_hour", 0));
        double snowMmPerHour = doubleValue(row, "snowMmPerHour", doubleValue(row, "snow_mm_per_hour", 0));
        boolean hail = booleanValue(row, "hail", false);
        boolean thunderstorm = booleanValue(row, "thunderstorm", false);
        int visibilityMeters = intValue(row, "visibilityMeters", intValue(row, "visibility_meters", 10_000));
        boolean fog = booleanValue(row, "fog", false);
        int cloudCoveragePercent = intValue(row, "cloudCoveragePercent", intValue(row, "cloud_coverage_percent", 20));
        int ceilingMeters = intValue(row, "ceilingMeters", intValue(row, "ceiling_meters", 4_000));
        String cloudLabel = value(row, "cloudLabel", value(row, "cloud_label", "Few Clouds"));
        String runwaySurface = value(row, "runwaySurface", value(row, "runway_surface", "DRY"));

        return new WeatherSnapshot(
                airportId,
                observedAt,
                new Temperature(temperatureCelsius, feelsLikeCelsius),
                new Wind(windSpeedKmh, windGustKmh, windDirectionDegrees),
                new Precipitation(rainMmPerHour, snowMmPerHour, hail, thunderstorm),
                new Visibility(visibilityMeters, fog),
                new CloudCondition(cloudCoveragePercent, ceilingMeters, cloudLabel),
                RunwaySurfaceCondition.fromCode(runwaySurface),
                WeatherSeverity.NORMAL
        );
    }

    private String value(Map<String, String> row, String key, String fallback) {
        String value = row.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private double doubleValue(Map<String, String> row, String key, double fallback) {
        String value = row.get(key);
        return value == null || value.isBlank() ? fallback : Double.parseDouble(value);
    }

    private int intValue(Map<String, String> row, String key, int fallback) {
        String value = row.get(key);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
    }

    private boolean booleanValue(Map<String, String> row, String key, boolean fallback) {
        String value = row.get(key);
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value);
    }
}
