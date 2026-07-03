package edu.uni.airportsim.simulation;

import java.util.Arrays;

public enum TimeMultiplier {
    X1(1, "x1"),
    X2(2, "x2"),
    X10(10, "x10"),
    X20(20, "x20");

    private final int factor;
    private final String label;

    TimeMultiplier(int factor, String label) {
        this.factor = factor;
        this.label = label;
    }

    public int factor() {
        return factor;
    }

    public String label() {
        return label;
    }

    public static TimeMultiplier fromLabel(String label) {
        return Arrays.stream(values())
                .filter(value -> value.label.equalsIgnoreCase(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported time multiplier: " + label));
    }
}
