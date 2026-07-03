package edu.uni.airportsim.weather;

import edu.uni.airportsim.domain.Airport;

import java.util.List;

public class WeatherService {
    private final WeatherImpactPolicy impactPolicy;

    public WeatherService() {
        this(new DefaultWeatherImpactPolicy());
    }

    public WeatherService(WeatherImpactPolicy impactPolicy) {
        this.impactPolicy = impactPolicy;
    }

    public List<WeatherAlert> evaluate(Airport airport, WeatherSnapshot snapshot) {
        return impactPolicy.evaluate(airport, snapshot);
    }
}
