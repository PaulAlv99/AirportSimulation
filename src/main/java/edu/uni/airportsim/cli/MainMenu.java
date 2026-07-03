package edu.uni.airportsim.cli;

import edu.uni.airportsim.domain.Airport;
import edu.uni.airportsim.domain.Flight;
import edu.uni.airportsim.io.AirportCsvImporter;
import edu.uni.airportsim.io.CsvParser;
import edu.uni.airportsim.io.FlightCsvImporter;
import edu.uni.airportsim.io.SyntheticDataGenerator;
import edu.uni.airportsim.io.WeatherCsvImporter;
import edu.uni.airportsim.logging.LogLevel;
import edu.uni.airportsim.logging.SimulationLogger;
import edu.uni.airportsim.persistence.SaveGameService;
import edu.uni.airportsim.simulation.SimulationClock;
import edu.uni.airportsim.simulation.SimulationEngine;
import edu.uni.airportsim.simulation.SimulationLifecycleState;
import edu.uni.airportsim.simulation.SimulationState;
import edu.uni.airportsim.weather.WeatherSnapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainMenu {
    private final Scanner scanner;
    private SimulationState state;
    private final Path dataDirectory;
    private final Path saveDirectory;
    private final Path logDirectory;
    private final SimulationLogger logger;
    private final SaveGameService saveGameService = new SaveGameService();
    private final Map<String, CliCommand> commands = new LinkedHashMap<>();

    public MainMenu(
            Scanner scanner,
            SimulationState state,
            Path dataDirectory,
            Path saveDirectory,
            Path logDirectory,
            SimulationLogger logger
    ) {
        this.scanner = scanner;
        this.state = state;
        this.dataDirectory = dataDirectory;
        this.saveDirectory = saveDirectory;
        this.logDirectory = logDirectory;
        this.logger = logger;
        registerCommands();
    }

    public void show() {
        boolean running = true;
        while (running) {
            printMenu();
            if (!scanner.hasNextLine()) {
                return;
            }
            String option = scanner.nextLine().trim();
            CliCommand command = commands.get(option);
            if (command == null) {
                System.out.println("Invalid option.");
            } else {
                running = command.execute();
            }
        }
    }

    public void printMenu() {
        System.out.println();
        System.out.println("=== Airport Simulation ===");
        commands.forEach((key, command) -> System.out.println(key + ". " + command.label()));
        System.out.print("Choose option: ");
    }

    private void registerCommands() {
        commands.put("1", command("Load data from CSV", () -> loadDataFromCsv()));
        commands.put("2", command("Generate sample CSV data", () -> generateSampleData()));
        commands.put("3", command("Start simulation", () -> startSimulation()));
        commands.put("4", command("Restore previous simulation", () -> restoreSimulation()));
        commands.put("5", command("View logs by day", () -> new LogViewerMenu(scanner, logDirectory).show()));
        commands.put("6", new CliCommand() {
            @Override
            public String label() {
                return "Exit";
            }

            @Override
            public boolean execute() {
                return false;
            }
        });
    }

    private CliCommand command(String label, Runnable action) {
        return new CliCommand() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public boolean execute() {
                action.run();
                return true;
            }
        };
    }

    private void loadDataFromCsv() {
        Path airportsFile = existingOrGenerated("world-airports.csv", "airports.csv");
        Path flightsFile = existingOrGenerated("airlines-flights.csv", "flights.csv");
        Path weatherFile = existingOrGenerated("weather.csv", "weather.csv");

        if (!Files.exists(airportsFile) || !Files.exists(flightsFile)) {
            System.out.println("CSV files not found. Place Kaggle CSV files in data/import or generate sample data first.");
            return;
        }

        CsvParser parser = new CsvParser();
        List<Airport> airports = new AirportCsvImporter(parser).importFrom(airportsFile);
        List<Flight> flights = new FlightCsvImporter(parser).importFrom(flightsFile);
        List<WeatherSnapshot> weather = Files.exists(weatherFile)
                ? new WeatherCsvImporter(parser).importFrom(weatherFile)
                : List.of();
        if (!airports.isEmpty()) {
            state.setAirport(airports.getFirst());
        }
        flights.forEach(state::addFlight);
        if (!weather.isEmpty()) {
            state.setWeatherSnapshot(weather.getFirst());
        }
        state.setLifecycleState(SimulationLifecycleState.LOADED);
        logger.log(LogLevel.INFO, "CSV", "Loaded " + airports.size() + " airports, " + flights.size() + " flights, and " + weather.size() + " weather snapshots");
        System.out.println("Loaded " + airports.size() + " airport(s), " + flights.size() + " flight(s), and " + weather.size() + " weather snapshot(s).");
    }

    private Path existingOrGenerated(String importName, String generatedName) {
        Path imported = dataDirectory.resolve("import").resolve(importName);
        if (Files.exists(imported)) {
            return imported;
        }
        return dataDirectory.resolve("generated").resolve(generatedName);
    }

    private void generateSampleData() {
        new SyntheticDataGenerator().generate(dataDirectory.resolve("generated"));
        logger.log(LogLevel.INFO, "CSV", "Generated sample CSV data");
        System.out.println("Sample CSV data generated in data/generated.");
    }

    private void startSimulation() {
        SimulationClock clock = new SimulationClock();
        SimulationEngine engine = new SimulationEngine(state, clock, logger);
        engine.start();
        new SimulationPanel(scanner, engine, saveDirectory, logger).show();
    }

    private void restoreSimulation() {
        Path saveFile = saveDirectory.resolve("simulation-state.json");
        if (!Files.exists(saveFile)) {
            System.out.println("No save file found at " + saveFile);
            return;
        }
        state = saveGameService.restore(saveFile);
        logger.log(LogLevel.INFO, "SAVE", "Restored simulation from " + saveFile);
        System.out.println("Simulation restored from " + saveFile);
    }
}
