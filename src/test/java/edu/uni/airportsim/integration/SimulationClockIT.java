package edu.uni.airportsim.integration;

import edu.uni.airportsim.simulation.SimulationClock;
import edu.uni.airportsim.simulation.TimeMultiplier;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationClockIT {
    @Test
    void supportsAllConfiguredMultipliers() {
        Instant realStart = Instant.parse("2026-01-01T00:00:00Z");
        LocalDateTime simulatedStart = LocalDateTime.of(2026, 1, 1, 0, 0);
        SimulationClock clock = new SimulationClock(Clock.fixed(realStart, ZoneOffset.UTC), simulatedStart);

        clock.setMultiplier(TimeMultiplier.X1);
        assertEquals(simulatedStart.plusSeconds(5), clock.currentSimulatedTime(realStart.plusSeconds(5)));

        clock.setMultiplier(TimeMultiplier.X2);
        assertEquals(simulatedStart.plusSeconds(10), clock.currentSimulatedTime(realStart.plusSeconds(5)));

        clock.setMultiplier(TimeMultiplier.X20);
        assertEquals(simulatedStart.plusSeconds(100), clock.currentSimulatedTime(realStart.plusSeconds(5)));
    }
}
