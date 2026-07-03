package edu.uni.airportsim.logging;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class LogReader {
    private final DailyLogFile dailyLogFile;

    public LogReader(Path logDirectory) {
        this.dailyLogFile = new DailyLogFile(logDirectory);
    }

    public List<LogEntry> readByDay(LocalDate date) {
        Path path = dailyLogFile.pathFor(date);
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            return Files.readAllLines(path)
                    .stream()
                    .filter(line -> !line.isBlank())
                    .map(LogEntry::parse)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read log file", exception);
        }
    }
}
