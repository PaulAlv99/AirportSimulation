package edu.uni.airportsim.io;

import edu.uni.airportsim.domain.Airline;
import edu.uni.airportsim.domain.Flight;
import edu.uni.airportsim.domain.FlightSchedule;
import edu.uni.airportsim.domain.Route;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class FlightCsvImporter implements CsvImporter<Flight> {
    private final CsvParser parser;

    public FlightCsvImporter(CsvParser parser) {
        this.parser = parser;
    }

    @Override
    public List<Flight> importFrom(Path csvFile) {
        return parser.parse(csvFile)
                .stream()
                .map(this::toFlight)
                .toList();
    }

    private Flight toFlight(Map<String, String> row) {
        String id = value(row, "id", "FLT-" + value(row, "flightNumber", "UNKNOWN"));
        String flightNumber = value(row, "flightNumber", value(row, "flight", id));
        String airlineName = value(row, "airlineName", value(row, "airline", "Sample Airline"));
        String airlineCode = value(row, "airlineCode", "SA");
        String origin = value(row, "origin", "LIS");
        String destination = value(row, "destination", "OPO");
        LocalDateTime departure = LocalDateTime.parse(value(row, "departure", "2026-01-01T08:00:00"));
        LocalDateTime arrival = LocalDateTime.parse(value(row, "arrival", departure.plusHours(2).toString()));

        Airline airline = new Airline("AIR-" + airlineCode, airlineName, airlineCode);
        return new Flight(id, flightNumber, airline, new Route(origin, destination), new FlightSchedule(departure, arrival));
    }

    private String value(Map<String, String> row, String key, String fallback) {
        String value = row.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
