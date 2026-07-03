package edu.uni.airportsim.weather;

public record Temperature(double celsius, double feelsLikeCelsius) {
    public static Temperature of(double celsius) {
        return new Temperature(celsius, celsius);
    }
}
