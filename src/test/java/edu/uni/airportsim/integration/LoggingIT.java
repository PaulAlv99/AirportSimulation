package edu.uni.airportsim.integration;

import edu.uni.airportsim.logging.LogEntry;
import edu.uni.airportsim.logging.LogLevel;
import edu.uni.airportsim.logging.LogReader;
import edu.uni.airportsim.logging.SimulationLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoggingIT {
    @TempDir
    Path tempDir;

    @Test
    void logManagementCanReadEntriesByDate() {
        Clock clock = Clock.fixed(Instant.parse("2026-02-03T09:00:00Z"), ZoneOffset.UTC);
        new SimulationLogger(tempDir, clock).log(LogLevel.INFO, "CLI", "opened log viewer");

        List<LogEntry> entries = new LogReader(tempDir).readByDay(LocalDate.of(2026, 2, 3));

        assertEquals(1, entries.size());
        assertEquals("opened log viewer", entries.getFirst().message());
    }
}
