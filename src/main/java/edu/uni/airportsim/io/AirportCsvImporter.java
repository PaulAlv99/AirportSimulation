package edu.uni.airportsim.io;

import edu.uni.airportsim.domain.Address;
import edu.uni.airportsim.domain.Airport;
import edu.uni.airportsim.domain.ControlTower;
import edu.uni.airportsim.domain.Gate;
import edu.uni.airportsim.domain.Lounge;
import edu.uni.airportsim.domain.ParkingArea;
import edu.uni.airportsim.domain.Runway;
import edu.uni.airportsim.domain.Terminal;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class AirportCsvImporter implements CsvImporter<Airport> {
    private final CsvParser parser;

    public AirportCsvImporter(CsvParser parser) {
        this.parser = parser;
    }

    @Override
    public List<Airport> importFrom(Path csvFile) {
        return parser.parse(csvFile)
                .stream()
                .map(this::toAirport)
                .toList();
    }

    private Airport toAirport(Map<String, String> row) {
        String id = value(row, "id", value(row, "ident", "APT-" + value(row, "code", "UNKNOWN")));
        String name = value(row, "name", "Airport " + id);
        String code = value(row, "code", value(row, "iata_code", id));
        String city = value(row, "city", value(row, "municipality", ""));
        String country = value(row, "country", value(row, "iso_country", ""));

        Airport airport = new Airport(id, name, code, new Address(city, country));
        airport.setControlTower(new ControlTower("TWR-" + id, name + " Control Tower"));

        Terminal terminal = new Terminal("TER-" + id + "-1", "Terminal 1");
        terminal.addGate(new Gate("GATE-" + id + "-A1", "Gate A1"));
        terminal.addLounge(new Lounge("LNG-" + id + "-1", "Main Lounge", 120));
        airport.addTerminal(terminal);
        airport.addRunway(new Runway("RWY-" + id + "-1", "Runway 1"));
        airport.addParkingArea(new ParkingArea("PARK-" + id + "-1", "Main Parking", 500));
        return airport;
    }

    private String value(Map<String, String> row, String key, String fallback) {
        String value = row.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
