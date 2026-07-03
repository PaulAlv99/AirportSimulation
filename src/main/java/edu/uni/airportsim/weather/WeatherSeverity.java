package edu.uni.airportsim.weather;

public final class WeatherSeverity {
    public static final WeatherSeverity NORMAL = new WeatherSeverity("NORMAL", "Normal", 0);
    public static final WeatherSeverity CAUTION = new WeatherSeverity("CAUTION", "Caution", 1);
    public static final WeatherSeverity SEVERE = new WeatherSeverity("SEVERE", "Severe", 2);
    public static final WeatherSeverity GROUND_STOP = new WeatherSeverity("GROUND_STOP", "Ground Stop", 3);

    private final String code;
    private final String label;
    private final int rank;

    public WeatherSeverity(String code, String label, int rank) {
        this.code = code;
        this.label = label;
        this.rank = rank;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public int rank() {
        return rank;
    }

    public WeatherSeverity max(WeatherSeverity other) {
        return other.rank > rank ? other : this;
    }

    public static WeatherSeverity fromCode(String code) {
        return switch (code) {
            case "CAUTION" -> CAUTION;
            case "SEVERE" -> SEVERE;
            case "GROUND_STOP" -> GROUND_STOP;
            default -> NORMAL;
        };
    }
}
