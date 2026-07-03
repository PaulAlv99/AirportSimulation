package edu.uni.airportsim.app;

import edu.uni.airportsim.cli.MainMenu;
import edu.uni.airportsim.logging.SimulationLogger;
import edu.uni.airportsim.simulation.SimulationState;

import java.nio.file.Path;
import java.util.Scanner;

public class AirportSimulationApp {
    private final Path dataDirectory;
    private final Path saveDirectory;
    private final Path logDirectory;

    public AirportSimulationApp() {
        this(Path.of("data"), Path.of("saves"), Path.of("logs"));
    }

    public AirportSimulationApp(Path dataDirectory, Path saveDirectory, Path logDirectory) {
        this.dataDirectory = dataDirectory;
        this.saveDirectory = saveDirectory;
        this.logDirectory = logDirectory;
    }

    public void run() {
        SimulationState state = new SimulationState();
        SimulationLogger logger = new SimulationLogger(logDirectory);
        try (Scanner scanner = new Scanner(System.in)) {
            new MainMenu(scanner, state, dataDirectory, saveDirectory, logDirectory, logger).show();
        }
    }
}
