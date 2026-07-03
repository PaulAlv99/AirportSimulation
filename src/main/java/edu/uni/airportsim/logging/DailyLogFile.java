package edu.uni.airportsim.logging;

import java.nio.file.Path;
import java.time.LocalDate;

public class DailyLogFile {
    private final Path logDirectory;

    public DailyLogFile(Path logDirectory) {
        this.logDirectory = logDirectory;
    }

    public Path pathFor(LocalDate date) {
        return logDirectory.resolve(date + ".log");
    }
}
