package edu.uni.airportsim.repository;

import edu.uni.airportsim.weather.WeatherSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class WeatherRepository {
    private final List<WeatherSnapshot> snapshots = new ArrayList<>();

    public void save(WeatherSnapshot snapshot) {
        snapshots.add(snapshot);
    }

    public Optional<WeatherSnapshot> latestForAirport(String airportId) {
        return snapshots.stream()
                .filter(snapshot -> snapshot.airportId().equals(airportId))
                .max(Comparator.comparing(WeatherSnapshot::observedAt));
    }

    public List<WeatherSnapshot> findAll() {
        return List.copyOf(snapshots);
    }
}
