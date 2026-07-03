package edu.uni.airportsim.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SyntheticDataGenerator {
    public void generate(Path outputDirectory) {
        try {
            Files.createDirectories(outputDirectory);
            Files.write(outputDirectory.resolve("airports.csv"), airportRows(), StandardCharsets.UTF_8);
            Files.write(outputDirectory.resolve("flights.csv"), flightRows(), StandardCharsets.UTF_8);
            Files.write(outputDirectory.resolve("weather.csv"), weatherRows(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to generate sample CSV data", exception);
        }
    }

    private List<String> airportRows() {
        return List.of(
                "id,name,code,city,country",
                "APT-LIS,Humberto Delgado Airport,LIS,Lisbon,Portugal",
                "APT-OPO,Francisco Sa Carneiro Airport,OPO,Porto,Portugal"
        );
    }

    private List<String> flightRows() {
        return List.of(
                "id,flightNumber,airlineName,airlineCode,origin,destination,departure,arrival",
                "FLT-TP100,TP100,TAP Air Portugal,TP,LIS,OPO,2026-01-01T08:00:00,2026-01-01T09:00:00",
                "FLT-TP200,TP200,TAP Air Portugal,TP,OPO,LIS,2026-01-01T10:00:00,2026-01-01T11:00:00"
        );
    }

    private List<String> weatherRows() {
        return List.of(
                "airportId,observedAt,temperatureCelsius,feelsLikeCelsius,windSpeedKmh,windGustKmh,windDirectionDegrees,rainMmPerHour,snowMmPerHour,hail,thunderstorm,visibilityMeters,fog,cloudCoveragePercent,ceilingMeters,cloudLabel,runwaySurface",
                "APT-LIS,2026-01-01T08:00:00,18,18,18,28,270,0,0,false,false,10000,false,20,4500,Few Clouds,DRY",
                "APT-OPO,2026-01-01T08:00:00,9,6,38,55,315,5,0,false,false,1800,false,80,900,Low Clouds,WET"
        );
    }
}
