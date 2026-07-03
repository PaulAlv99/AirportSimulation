package edu.uni.airportsim.cli;

import edu.uni.airportsim.logging.LogEntry;
import edu.uni.airportsim.logging.LogReader;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class LogViewerMenu {
    private final Scanner scanner;
    private final LogReader logReader;

    public LogViewerMenu(Scanner scanner, Path logDirectory) {
        this.scanner = scanner;
        this.logReader = new LogReader(logDirectory);
    }

    public void show() {
        System.out.print("Enter log date (yyyy-MM-dd): ");
        if (!scanner.hasNextLine()) {
            return;
        }
        LocalDate date = LocalDate.parse(scanner.nextLine().trim());
        List<LogEntry> entries = logReader.readByDay(date);
        if (entries.isEmpty()) {
            System.out.println("No logs found for " + date);
            return;
        }
        entries.forEach(entry -> System.out.println(entry.format()));
    }
}
