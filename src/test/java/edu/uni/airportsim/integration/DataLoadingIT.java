package edu.uni.airportsim.integration;

import edu.uni.airportsim.domain.Airport;
import edu.uni.airportsim.domain.Flight;
import edu.uni.airportsim.io.AirportCsvImporter;
import edu.uni.airportsim.io.CsvParser;
import edu.uni.airportsim.io.FlightCsvImporter;
import edu.uni.airportsim.io.SyntheticDataGenerator;
import edu.uni.airportsim.io.WeatherCsvImporter;
import edu.uni.airportsim.weather.WeatherSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataLoadingIT {
    @TempDir
    Path tempDir;

    @Test
    void generatedCsvFilesLoadIntoDomainStructures() {
        new SyntheticDataGenerator().generate(tempDir);
        CsvParser parser = new CsvParser();

        List<Airport> airports = new AirportCsvImporter(parser).importFrom(tempDir.resolve("airports.csv"));
        List<Flight> flights = new FlightCsvImporter(parser).importFrom(tempDir.resolve("flights.csv"));
        List<WeatherSnapshot> weather = new WeatherCsvImporter(parser).importFrom(tempDir.resolve("weather.csv"));

        assertEquals(2, airports.size());
        assertEquals("LIS", airports.getFirst().getCode());
        assertEquals(2, flights.size());
        assertEquals("TP100", flights.getFirst().getFlightNumber());
        assertEquals(2, weather.size());
        assertEquals("APT-LIS", weather.getFirst().airportId());
    }
}
