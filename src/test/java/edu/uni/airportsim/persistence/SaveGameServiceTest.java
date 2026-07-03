package edu.uni.airportsim.persistence;

import edu.uni.airportsim.domain.Address;
import edu.uni.airportsim.domain.Airline;
import edu.uni.airportsim.domain.Airport;
import edu.uni.airportsim.domain.Flight;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaveGameServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndRestoresMinimalSimulationState() {
        SimulationState state = new SimulationState();
        state.setLifecycleState(SimulationLifecycleState.RUNNING);
        state.setTimeMultiplier(TimeMultiplier.X10);
        state.setSimulatedTime(LocalDateTime.of(2026, 1, 1, 12, 0));
        state.setAirport(new Airport("APT-LIS", "Humberto Delgado Airport", "LIS", new Address("Lisbon", "Portugal")));
        state.addFlight(new Flight(
                "FLT-TP100",
                "TP100",
                new Airline("AIR-TP", "TAP Air Portugal", "TP"),
                new Route("LIS", "OPO"),
                new FlightSchedule(LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 9, 0))
        ));
        state.setWeatherSnapshot(new WeatherSnapshot(
                "APT-LIS",
                LocalDateTime.of(2026, 1, 1, 8, 0),
                new Temperature(18, 17),
                new Wind(30, 42, 270),
                new Precipitation(1.5, 0, false, false),
                new Visibility(9000, false),
                new CloudCondition(40, 3000, "Scattered"),
                RunwaySurfaceCondition.WET,
                WeatherSeverity.CAUTION
        ));

        Path saveFile = tempDir.resolve("simulation-state.json");
        SaveGameService service = new SaveGameService();
        service.save(state, saveFile);
        SimulationState restored = service.restore(saveFile);

        assertEquals(SimulationLifecycleState.RUNNING.code(), restored.getLifecycleState().code());
        assertEquals(TimeMultiplier.X10, restored.getTimeMultiplier());
        assertEquals("LIS", restored.getAirport().getCode());
        assertEquals(1, restored.getFlights().size());
        assertEquals("TP100", restored.getFlights().getFirst().getFlightNumber());
        assertEquals("WET", restored.getWeatherSnapshot().runwaySurfaceCondition().code());
    }
}
