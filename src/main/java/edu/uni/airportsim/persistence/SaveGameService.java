package edu.uni.airportsim.persistence;

import edu.uni.airportsim.domain.Address;
import edu.uni.airportsim.domain.Airline;
import edu.uni.airportsim.domain.Airport;
import edu.uni.airportsim.domain.Flight;
import edu.uni.airportsim.domain.FlightState;
import edu.uni.airportsim.domain.FlightSchedule;
import edu.uni.airportsim.domain.Route;
import edu.uni.airportsim.simulation.SimulationLifecycleState;
import edu.uni.airportsim.simulation.SimulationState;
import edu.uni.airportsim.simulation.TimeMultiplier;
import edu.uni.airportsim.weather.CloudCondition;
import edu.uni.airportsim.weather.Precipitation;
import edu.uni.airportsim.weather.RunwaySurfaceCondition;
import edu.uni.airportsim.weather.Temperature;
import edu.uni.airportsim.weather.Visibility;
import edu.uni.airportsim.weather.WeatherSeverity;
import edu.uni.airportsim.weather.WeatherSnapshot;
import edu.uni.airportsim.weather.Wind;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class SaveGameService {
    private final CustomJsonParser parser = new CustomJsonParser();
    private final CustomJsonWriter writer = new CustomJsonWriter();

    public void save(SimulationState state, Path file) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, writer.write(toJson(state)), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save simulation state", exception);
        }
    }

    public SimulationState restore(Path file) {
        try {
            JsonObject root = parser.parse(Files.readString(file, StandardCharsets.UTF_8)).asObject();
            return fromJson(root);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to restore simulation state", exception);
        }
    }

    public JsonObject toJson(SimulationState state) {
        JsonObject root = new JsonObject()
                .put("lifecycleState", JsonValue.of(state.getLifecycleState().code()))
                .put("simulatedTime", JsonValue.of(state.getSimulatedTime().toString()))
                .put("timeMultiplier", JsonValue.of(state.getTimeMultiplier().label()));

        if (state.getAirport() != null) {
            Airport airport = state.getAirport();
            root.put("airport", new JsonObject()
                    .put("id", JsonValue.of(airport.getId()))
                    .put("name", JsonValue.of(airport.getName()))
                    .put("code", JsonValue.of(airport.getCode()))
                    .put("city", JsonValue.of(airport.getAddress() == null ? "" : airport.getAddress().city()))
                    .put("country", JsonValue.of(airport.getAddress() == null ? "" : airport.getAddress().country())));
        }

        JsonArray flights = new JsonArray();
        for (Flight flight : state.getFlights()) {
            flights.add(new JsonObject()
                    .put("id", JsonValue.of(flight.getId()))
                    .put("flightNumber", JsonValue.of(flight.getFlightNumber()))
                    .put("state", JsonValue.of(flight.getState().code()))
                    .put("airlineName", JsonValue.of(flight.getAirline().getName()))
                    .put("airlineCode", JsonValue.of(flight.getAirline().getCode()))
                    .put("origin", JsonValue.of(flight.getRoute().originAirportCode()))
                    .put("destination", JsonValue.of(flight.getRoute().destinationAirportCode()))
                    .put("departure", JsonValue.of(flight.getSchedule().departureTime().toString()))
                    .put("arrival", JsonValue.of(flight.getSchedule().arrivalTime().toString())));
        }
        root.put("flights", flights);

        if (state.getWeatherSnapshot() != null) {
            root.put("weather", weatherToJson(state.getWeatherSnapshot()));
        }
        return root;
    }

    private SimulationState fromJson(JsonObject root) {
        SimulationState state = new SimulationState();
        state.setLifecycleState(SimulationLifecycleState.fromCode(root.getString("lifecycleState", "CREATED")));
        state.setSimulatedTime(LocalDateTime.parse(root.getString("simulatedTime", LocalDateTime.now().toString())));
        state.setTimeMultiplier(TimeMultiplier.fromLabel(root.getString("timeMultiplier", TimeMultiplier.X1.label())));

        if (root.contains("airport")) {
            JsonObject airportJson = root.getObject("airport");
            state.setAirport(new Airport(
                    airportJson.getString("id", "APT-RESTORED"),
                    airportJson.getString("name", "Restored Airport"),
                    airportJson.getString("code", "RST"),
                    new Address(airportJson.getString("city", ""), airportJson.getString("country", ""))
            ));
        }

        for (JsonValue value : root.getArray("flights").values()) {
            JsonObject flightJson = value.asObject();
            Airline airline = new Airline(
                    "AIR-" + flightJson.getString("airlineCode", "RST"),
                    flightJson.getString("airlineName", "Restored Airline"),
                    flightJson.getString("airlineCode", "RST")
            );
            Flight flight = new Flight(
                    flightJson.getString("id", "FLT-RESTORED"),
                    flightJson.getString("flightNumber", "RST1"),
                    airline,
                    new Route(flightJson.getString("origin", "AAA"), flightJson.getString("destination", "BBB")),
                    new FlightSchedule(
                            LocalDateTime.parse(flightJson.getString("departure", "2026-01-01T00:00:00")),
                            LocalDateTime.parse(flightJson.getString("arrival", "2026-01-01T01:00:00"))
                    )
            );
            flight.setState(FlightState.fromCode(flightJson.getString("state", "SCHEDULED")));
            state.addFlight(flight);
        }
        if (root.contains("weather")) {
            state.setWeatherSnapshot(weatherFromJson(root.getObject("weather")));
        }
        return state;
    }

    private JsonObject weatherToJson(WeatherSnapshot weather) {
        return new JsonObject()
                .put("airportId", JsonValue.of(weather.airportId()))
                .put("observedAt", JsonValue.of(weather.observedAt().toString()))
                .put("temperatureCelsius", JsonValue.of(weather.temperature().celsius()))
                .put("feelsLikeCelsius", JsonValue.of(weather.temperature().feelsLikeCelsius()))
                .put("windSpeedKmh", JsonValue.of(weather.wind().speedKmh()))
                .put("windGustKmh", JsonValue.of(weather.wind().gustKmh()))
                .put("windDirectionDegrees", JsonValue.of(weather.wind().directionDegrees()))
                .put("rainMmPerHour", JsonValue.of(weather.precipitation().rainMmPerHour()))
                .put("snowMmPerHour", JsonValue.of(weather.precipitation().snowMmPerHour()))
                .put("hail", JsonValue.of(weather.precipitation().hail()))
                .put("thunderstorm", JsonValue.of(weather.precipitation().thunderstorm()))
                .put("visibilityMeters", JsonValue.of(weather.visibility().meters()))
                .put("fog", JsonValue.of(weather.visibility().fog()))
                .put("cloudCoveragePercent", JsonValue.of(weather.cloudCondition().coveragePercent()))
                .put("ceilingMeters", JsonValue.of(weather.cloudCondition().ceilingMeters()))
                .put("cloudLabel", JsonValue.of(weather.cloudCondition().label()))
                .put("runwaySurface", JsonValue.of(weather.runwaySurfaceCondition().code()))
                .put("weatherSeverity", JsonValue.of(weather.severity().code()));
    }

    private WeatherSnapshot weatherFromJson(JsonObject weather) {
        return new WeatherSnapshot(
                weather.getString("airportId", "APT-RESTORED"),
                LocalDateTime.parse(weather.getString("observedAt", "2026-01-01T00:00:00")),
                new Temperature(
                        weather.get("temperatureCelsius").asNumber().doubleValue(),
                        weather.get("feelsLikeCelsius").asNumber().doubleValue()
                ),
                new Wind(
                        weather.get("windSpeedKmh").asNumber().doubleValue(),
                        weather.get("windGustKmh").asNumber().doubleValue(),
                        weather.get("windDirectionDegrees").asNumber().intValue()
                ),
                new Precipitation(
                        weather.get("rainMmPerHour").asNumber().doubleValue(),
                        weather.get("snowMmPerHour").asNumber().doubleValue(),
                        weather.get("hail").asBoolean(),
                        weather.get("thunderstorm").asBoolean()
                ),
                new Visibility(weather.get("visibilityMeters").asNumber().intValue(), weather.get("fog").asBoolean()),
                new CloudCondition(
                        weather.get("cloudCoveragePercent").asNumber().intValue(),
                        weather.get("ceilingMeters").asNumber().intValue(),
                        weather.getString("cloudLabel", "")
                ),
                RunwaySurfaceCondition.fromCode(weather.getString("runwaySurface", "DRY")),
                WeatherSeverity.fromCode(weather.getString("weatherSeverity", "NORMAL"))
        );
    }
}
