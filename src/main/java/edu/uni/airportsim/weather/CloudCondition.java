package edu.uni.airportsim.weather;

public record CloudCondition(int coveragePercent, int ceilingMeters, String label) {
    public CloudCondition {
        if (coveragePercent < 0 || coveragePercent > 100) {
            throw new IllegalArgumentException("coveragePercent must be between 0 and 100");
        }
        if (ceilingMeters < 0) {
            throw new IllegalArgumentException("ceilingMeters must not be negative");
        }
        label = label == null ? "" : label;
    }
}
