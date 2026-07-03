package edu.uni.airportsim.simulation;

import edu.uni.airportsim.domain.Airport;
import edu.uni.airportsim.domain.Flight;
import edu.uni.airportsim.domain.Reservation;
import edu.uni.airportsim.weather.WeatherAlert;
import edu.uni.airportsim.weather.WeatherSnapshot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimulationState {
    private Airport airport;
    private final List<Flight> flights = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private final List<WeatherAlert> weatherAlerts = new ArrayList<>();
    private SimulationLifecycleState lifecycleState = SimulationLifecycleState.CREATED;
    private LocalDateTime simulatedTime = LocalDateTime.now();
    private TimeMultiplier timeMultiplier = TimeMultiplier.X1;
    private WeatherSnapshot weatherSnapshot;

    public Airport getAirport() {
        return airport;
    }

    public void setAirport(Airport airport) {
        this.airport = airport;
    }

    public List<Flight> getFlights() {
        return Collections.unmodifiableList(flights);
    }

    public void addFlight(Flight flight) {
        flights.add(flight);
    }

    public List<Reservation> getReservations() {
        return Collections.unmodifiableList(reservations);
    }

    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
    }

    public List<WeatherAlert> getWeatherAlerts() {
        return Collections.unmodifiableList(weatherAlerts);
    }

    public void replaceWeatherAlerts(List<WeatherAlert> alerts) {
        weatherAlerts.clear();
        weatherAlerts.addAll(alerts);
    }

    public WeatherSnapshot getWeatherSnapshot() {
        return weatherSnapshot;
    }

    public void setWeatherSnapshot(WeatherSnapshot weatherSnapshot) {
        this.weatherSnapshot = weatherSnapshot;
    }

    public SimulationLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(SimulationLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public LocalDateTime getSimulatedTime() {
        return simulatedTime;
    }

    public void setSimulatedTime(LocalDateTime simulatedTime) {
        this.simulatedTime = simulatedTime;
    }

    public TimeMultiplier getTimeMultiplier() {
        return timeMultiplier;
    }

    public void setTimeMultiplier(TimeMultiplier timeMultiplier) {
        this.timeMultiplier = timeMultiplier;
    }
}
