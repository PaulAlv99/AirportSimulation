package edu.uni.airportsim.simulation;

import edu.uni.airportsim.logging.LogLevel;
import edu.uni.airportsim.logging.SimulationLogger;
import edu.uni.airportsim.weather.WeatherAlert;
import edu.uni.airportsim.weather.WeatherService;

import java.util.List;

public class SimulationEngine {
    private final SimulationState state;
    private final SimulationClock clock;
    private final SimulationLogger logger;
    private final WeatherService weatherService;

    public SimulationEngine(SimulationState state, SimulationClock clock, SimulationLogger logger) {
        this(state, clock, logger, new WeatherService());
    }

    public SimulationEngine(SimulationState state, SimulationClock clock, SimulationLogger logger, WeatherService weatherService) {
        this.state = state;
        this.clock = clock;
        this.logger = logger;
        this.weatherService = weatherService;
    }

    public SimulationState getState() {
        return state;
    }

    public SimulationClock getClock() {
        return clock;
    }

    public void start() {
        state.setLifecycleState(SimulationLifecycleState.RUNNING);
        logger.log(LogLevel.INFO, "SIMULATION", "Simulation started");
    }

    public void pause() {
        state.setLifecycleState(SimulationLifecycleState.PAUSED);
        logger.log(LogLevel.INFO, "SIMULATION", "Simulation paused");
    }

    public void stop() {
        state.setLifecycleState(SimulationLifecycleState.STOPPED);
        logger.log(LogLevel.INFO, "SIMULATION", "Simulation stopped");
    }

    public SimulationEvent tick() {
        state.setSimulatedTime(clock.now());
        state.setTimeMultiplier(clock.getMultiplier());
        evaluateWeather();
        return new SimulationEvent(state.getSimulatedTime(), "CLOCK", "Simulation tick at " + state.getTimeMultiplier().label());
    }

    private void evaluateWeather() {
        if (state.getAirport() == null || state.getWeatherSnapshot() == null) {
            return;
        }
        List<WeatherAlert> alerts = weatherService.evaluate(state.getAirport(), state.getWeatherSnapshot());
        state.replaceWeatherAlerts(alerts);
        for (WeatherAlert alert : alerts) {
            logger.log(LogLevel.INFO, "WEATHER", alert.severity().code() + " - " + alert.message());
        }
    }
}
