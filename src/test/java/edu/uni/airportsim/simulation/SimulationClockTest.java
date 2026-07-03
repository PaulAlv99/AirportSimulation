package edu.uni.airportsim.simulation;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationClockTest {
    @Test
    void appliesTimeMultiplierToElapsedRealTime() {
        Instant realStart = Instant.parse("2026-01-01T00:00:00Z");
        LocalDateTime simulatedStart = LocalDateTime.of(2026, 1, 1, 8, 0);
        SimulationClock clock = new SimulationClock(Clock.fixed(realStart, ZoneOffset.UTC), simulatedStart);

        clock.setMultiplier(TimeMultiplier.X10);

        assertEquals(
                LocalDateTime.of(2026, 1, 1, 8, 1, 40),
                clock.currentSimulatedTime(realStart.plusSeconds(10))
        );
    }
}
