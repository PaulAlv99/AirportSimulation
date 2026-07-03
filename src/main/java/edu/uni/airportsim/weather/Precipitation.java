package edu.uni.airportsim.weather;

public record Precipitation(double rainMmPerHour, double snowMmPerHour, boolean hail, boolean thunderstorm) {
    public Precipitation {
        if (rainMmPerHour < 0 || snowMmPerHour < 0) {
            throw new IllegalArgumentException("precipitation values must not be negative");
        }
    }

    public boolean hasRain() {
        return rainMmPerHour > 0;
    }

    public boolean hasSnow() {
        return snowMmPerHour > 0;
    }
}
