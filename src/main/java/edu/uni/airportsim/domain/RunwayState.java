package edu.uni.airportsim.domain;

public final class RunwayState {
    public static final RunwayState AVAILABLE = new RunwayState("AVAILABLE", "Available", true);
    public static final RunwayState IN_USE = new RunwayState("IN_USE", "In Use", false);
    public static final RunwayState MAINTENANCE = new RunwayState("MAINTENANCE", "Maintenance", false);
    public static final RunwayState CLOSED = new RunwayState("CLOSED", "Closed", false);

    private final String code;
    private final String label;
    private final boolean operational;

    public RunwayState(String code, String label, boolean operational) {
        this.code = BaseEntity.requireText(code, "code");
        this.label = BaseEntity.requireText(label, "label");
        this.operational = operational;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean isOperational() {
        return operational;
    }

    public static RunwayState fromCode(String code) {
        return switch (code) {
            case "IN_USE" -> IN_USE;
            case "MAINTENANCE" -> MAINTENANCE;
            case "CLOSED" -> CLOSED;
            default -> AVAILABLE;
        };
    }
}
