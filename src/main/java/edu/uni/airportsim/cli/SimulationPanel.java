package edu.uni.airportsim.cli;

import edu.uni.airportsim.logging.LogLevel;
import edu.uni.airportsim.logging.SimulationLogger;
import edu.uni.airportsim.persistence.SaveGameService;
import edu.uni.airportsim.simulation.SimulationEngine;
import edu.uni.airportsim.simulation.SimulationEvent;
import edu.uni.airportsim.simulation.TimeMultiplier;
import edu.uni.airportsim.weather.WeatherAlert;
import edu.uni.airportsim.weather.WeatherSnapshot;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class SimulationPanel {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Scanner scanner;
    private final SimulationEngine engine;
    private final Path saveDirectory;
    private final SimulationLogger logger;
    private final SaveGameService saveGameService = new SaveGameService();
    private final Map<String, CliCommand> commands = new LinkedHashMap<>();

    public SimulationPanel(Scanner scanner, SimulationEngine engine, Path saveDirectory, SimulationLogger logger) {
        this.scanner = scanner;
        this.engine = engine;
        this.saveDirectory = saveDirectory;
        this.logger = logger;
        registerCommands();
    }

    public void show() {
        boolean running = true;
        while (running) {
            SimulationEvent event = engine.tick();
            printStatus();
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
            logger.log(LogLevel.DEBUG, event.category(), event.message());
        }
    }

    private void printStatus() {
        System.out.println();
        System.out.println("=== Simulation Panel ===");
        System.out.println("Real time: " + engine.getClock().realNow().atZone(ZoneId.systemDefault()).format(TIME_FORMAT));
        System.out.println("Simulated time: " + engine.getState().getSimulatedTime().format(TIME_FORMAT));
        System.out.println("Multiplier: " + engine.getClock().getMultiplier().label());
        System.out.println("Flights loaded: " + engine.getState().getFlights().size());
        if (engine.getState().getAirport() != null) {
            System.out.println("Airport: " + engine.getState().getAirport().getDisplayName());
            System.out.println("Runways: " + engine.getState().getAirport().getRunways().size());
            System.out.println("Terminals: " + engine.getState().getAirport().getTerminals().size());
        }
        WeatherSnapshot weather = engine.getState().getWeatherSnapshot();
        if (weather != null) {
            System.out.println("Weather: " + weather.temperature().celsius() + "C, wind "
                    + weather.wind().speedKmh() + " km/h " + weather.wind().compassDirection()
                    + ", visibility " + weather.visibility().meters() + " m, severity "
                    + weather.severity().label());
        }
    }

    private void printMenu() {
        commands.forEach((key, command) -> System.out.println(key + ". " + command.label()));
        System.out.print("Choose option: ");
    }

    private void registerCommands() {
        commands.put("1", command("Change time multiplier", () -> changeMultiplier()));
        commands.put("2", command("Manage crew", () -> new CrewManagementMenu(scanner).show()));
        commands.put("3", command("Manage passengers and reservations", () -> new PassengerManagementMenu(scanner).show()));
        commands.put("4", command("Print tickets and boarding passes", () -> new TicketManagementMenu(scanner).show()));
        commands.put("5", command("View weather details", () -> printWeatherDetails()));
        commands.put("6", command("View checks, baggage, and ground operations", () -> printOperationsPlaceholder()));
        commands.put("7", command("Save current simulation", () -> saveSimulation()));
        commands.put("8", new CliCommand() {
            @Override
            public String label() {
                return "Return to main panel";
            }

            @Override
            public boolean execute() {
                engine.pause();
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

    private void changeMultiplier() {
        System.out.print("Enter multiplier (x1, x2, x10, x20): ");
        if (!scanner.hasNextLine()) {
            return;
        }
        try {
            TimeMultiplier multiplier = TimeMultiplier.fromLabel(scanner.nextLine().trim());
            engine.getClock().setMultiplier(multiplier);
            logger.log(LogLevel.INFO, "CLOCK", "Changed multiplier to " + multiplier.label());
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }
    }

    private void printOperationsPlaceholder() {
        System.out.println("Check-in, security, customs, immigration, baggage, and ground operations are scaffolded.");
    }

    private void printWeatherDetails() {
        WeatherSnapshot weather = engine.getState().getWeatherSnapshot();
        if (weather == null) {
            System.out.println("No weather snapshot loaded.");
            return;
        }
        System.out.println("Weather for airport " + weather.airportId());
        System.out.println("Observed at: " + weather.observedAt());
        System.out.println("Temperature: " + weather.temperature().celsius() + "C, feels like " + weather.temperature().feelsLikeCelsius() + "C");
        System.out.println("Wind: " + weather.wind().speedKmh() + " km/h, gust " + weather.wind().gustKmh()
                + " km/h, direction " + weather.wind().directionDegrees() + " degrees (" + weather.wind().compassDirection() + ")");
        System.out.println("Rain: " + weather.precipitation().rainMmPerHour() + " mm/h");
        System.out.println("Snow: " + weather.precipitation().snowMmPerHour() + " mm/h");
        System.out.println("Hail: " + weather.precipitation().hail());
        System.out.println("Thunderstorm: " + weather.precipitation().thunderstorm());
        System.out.println("Visibility: " + weather.visibility().meters() + " m, fog: " + weather.visibility().fog());
        System.out.println("Clouds: " + weather.cloudCondition().label() + ", coverage "
                + weather.cloudCondition().coveragePercent() + "%, ceiling " + weather.cloudCondition().ceilingMeters() + " m");
        System.out.println("Runway surface: " + weather.runwaySurfaceCondition().displayName());
        if (engine.getState().getWeatherAlerts().isEmpty()) {
            System.out.println("No active weather alerts.");
        } else {
            System.out.println("Active weather alerts:");
            for (WeatherAlert alert : engine.getState().getWeatherAlerts()) {
                System.out.println("- " + alert.severity().label() + ": " + alert.message());
            }
        }
    }

    private void saveSimulation() {
        Path saveFile = saveDirectory.resolve("simulation-state.json");
        saveGameService.save(engine.getState(), saveFile);
        logger.log(LogLevel.INFO, "SAVE", "Saved simulation to " + saveFile);
        System.out.println("Simulation saved to " + saveFile);
    }
}
