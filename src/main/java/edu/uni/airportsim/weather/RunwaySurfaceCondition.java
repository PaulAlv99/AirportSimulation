package edu.uni.airportsim.weather;

public record RunwaySurfaceCondition(String code, String displayName, double brakingFactor) {
    public static final RunwaySurfaceCondition DRY = new RunwaySurfaceCondition("DRY", "Dry", 1.0);
    public static final RunwaySurfaceCondition WET = new RunwaySurfaceCondition("WET", "Wet", 0.75);
    public static final RunwaySurfaceCondition SNOW = new RunwaySurfaceCondition("SNOW", "Snow", 0.55);
    public static final RunwaySurfaceCondition ICE = new RunwaySurfaceCondition("ICE", "Ice", 0.35);
    public static final RunwaySurfaceCondition SLUSH = new RunwaySurfaceCondition("SLUSH", "Slush", 0.45);

    public RunwaySurfaceCondition {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        displayName = displayName == null || displayName.isBlank() ? code : displayName;
        if (brakingFactor < 0 || brakingFactor > 1) {
            throw new IllegalArgumentException("brakingFactor must be between 0 and 1");
        }
    }

    public boolean isContaminated() {
        return brakingFactor < 0.7;
    }

    public static RunwaySurfaceCondition fromCode(String code) {
        return switch (code) {
            case "WET" -> WET;
            case "SNOW" -> SNOW;
            case "ICE" -> ICE;
            case "SLUSH" -> SLUSH;
            default -> DRY;
        };
    }
}
