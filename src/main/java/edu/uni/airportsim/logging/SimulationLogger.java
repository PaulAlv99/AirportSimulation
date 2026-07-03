package edu.uni.airportsim.logging;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public class SimulationLogger {
    private final Path logDirectory;
    private final Clock clock;
    private final DailyLogFile dailyLogFile;

    public SimulationLogger(Path logDirectory) {
        this(logDirectory, Clock.systemDefaultZone());
    }

    public SimulationLogger(Path logDirectory, Clock clock) {
        this.logDirectory = Objects.requireNonNull(logDirectory, "logDirectory");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.dailyLogFile = new DailyLogFile(logDirectory);
    }

    public void log(LogLevel level, String category, String message) {
        LogEntry entry = new LogEntry(clock.instant(), level, category, message);
        Path path = dailyLogFile.pathFor(LocalDate.now(clock));
        try {
            Files.createDirectories(logDirectory);
            Files.writeString(
                    path,
                    entry.format() + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write log entry", exception);
        }
    }
}
