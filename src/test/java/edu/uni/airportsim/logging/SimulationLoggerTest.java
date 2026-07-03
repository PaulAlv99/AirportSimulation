package edu.uni.airportsim.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationLoggerTest {
    @TempDir
    Path tempDir;

    @Test
    void writesAndReadsLogsByDay() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-02T10:15:30Z"), ZoneOffset.UTC);
        SimulationLogger logger = new SimulationLogger(tempDir, clock);

        logger.log(LogLevel.INFO, "TEST", "message");
        List<LogEntry> entries = new LogReader(tempDir).readByDay(LocalDate.of(2026, 1, 2));

        assertEquals(1, entries.size());
        assertEquals(LogLevel.INFO, entries.getFirst().level());
        assertEquals("TEST", entries.getFirst().category());
        assertEquals("message", entries.getFirst().message());
    }
}
