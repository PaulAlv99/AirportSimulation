package edu.uni.airportsim.io;

import edu.uni.airportsim.weather.WeatherSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherCsvImporterTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesWeatherCsvRows() throws IOException {
        Path file = tempDir.resolve("weather.csv");
        Files.writeString(file, """
                airportId,observedAt,temperatureCelsius,feelsLikeCelsius,windSpeedKmh,windGustKmh,windDirectionDegrees,rainMmPerHour,snowMmPerHour,hail,thunderstorm,visibilityMeters,fog,cloudCoveragePercent,ceilingMeters,cloudLabel,runwaySurface
                APT-LIS,2026-01-01T08:00:00,18,17,20,35,270,1,0,false,false,9000,false,40,3000,Scattered,WET
                """);

        List<WeatherSnapshot> snapshots = new WeatherCsvImporter(new CsvParser()).importFrom(file);

        assertEquals(1, snapshots.size());
        assertEquals("APT-LIS", snapshots.getFirst().airportId());
        assertEquals("WET", snapshots.getFirst().runwaySurfaceCondition().code());
    }
}
