package edu.uni.airportsim.weather;

public record Visibility(int meters, boolean fog) {
    public Visibility {
        if (meters < 0) {
            throw new IllegalArgumentException("visibility meters must not be negative");
        }
    }

    public boolean isLowVisibility() {
        return meters < 1000 || fog;
    }
}
