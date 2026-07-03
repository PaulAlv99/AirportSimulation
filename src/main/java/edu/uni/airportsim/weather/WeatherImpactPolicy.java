package edu.uni.airportsim.weather;

import edu.uni.airportsim.domain.Airport;

import java.util.List;

public interface WeatherImpactPolicy {
    List<WeatherAlert> evaluate(Airport airport, WeatherSnapshot snapshot);
}
