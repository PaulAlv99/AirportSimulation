package edu.uni.airportsim.domain;

public final class AircraftState {
    public static final AircraftState AVAILABLE = new AircraftState("AVAILABLE", "Available", true);
    public static final AircraftState BOARDING = new AircraftState("BOARDING", "Boarding", true);
    public static final AircraftState IN_FLIGHT = new AircraftState("IN_FLIGHT", "In Flight", false);
    public static final AircraftState MAINTENANCE = new AircraftState("MAINTENANCE", "Maintenance", false);
    public static final AircraftState GROUNDED = new AircraftState("GROUNDED", "Grounded", false);

    private final String code;
    private final String label;
    private final boolean assignable;

    public AircraftState(String code, String label, boolean assignable) {
        this.code = BaseEntity.requireText(code, "code");
        this.label = BaseEntity.requireText(label, "label");
        this.assignable = assignable;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean isAssignable() {
        return assignable;
    }

    public static AircraftState fromCode(String code) {
        return switch (code) {
            case "BOARDING" -> BOARDING;
            case "IN_FLIGHT" -> IN_FLIGHT;
            case "MAINTENANCE" -> MAINTENANCE;
            case "GROUNDED" -> GROUNDED;
            default -> AVAILABLE;
        };
    }
}
