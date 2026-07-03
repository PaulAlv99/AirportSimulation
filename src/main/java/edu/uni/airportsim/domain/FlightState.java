package edu.uni.airportsim.domain;

import java.util.Set;

public final class FlightState {
    public static final FlightState SCHEDULED = new FlightState("SCHEDULED", "Scheduled", Set.of("CHECK_IN_OPEN", "DELAYED", "CANCELLED"));
    public static final FlightState CHECK_IN_OPEN = new FlightState("CHECK_IN_OPEN", "Check-In Open", Set.of("BOARDING", "DELAYED", "CANCELLED"));
    public static final FlightState BOARDING = new FlightState("BOARDING", "Boarding", Set.of("DEPARTED", "DELAYED", "CANCELLED"));
    public static final FlightState DEPARTED = new FlightState("DEPARTED", "Departed", Set.of("ARRIVED"));
    public static final FlightState ARRIVED = new FlightState("ARRIVED", "Arrived", Set.of());
    public static final FlightState DELAYED = new FlightState("DELAYED", "Delayed", Set.of("CHECK_IN_OPEN", "BOARDING", "CANCELLED"));
    public static final FlightState CANCELLED = new FlightState("CANCELLED", "Cancelled", Set.of());

    private final String code;
    private final String label;
    private final Set<String> allowedTransitions;

    public FlightState(String code, String label, Set<String> allowedTransitions) {
        this.code = BaseEntity.requireText(code, "code");
        this.label = BaseEntity.requireText(label, "label");
        this.allowedTransitions = Set.copyOf(allowedTransitions);
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean canTransitionTo(FlightState nextState) {
        return nextState != null && allowedTransitions.contains(nextState.code);
    }

    public static FlightState fromCode(String code) {
        return switch (code) {
            case "CHECK_IN_OPEN" -> CHECK_IN_OPEN;
            case "BOARDING" -> BOARDING;
            case "DEPARTED" -> DEPARTED;
            case "ARRIVED" -> ARRIVED;
            case "DELAYED" -> DELAYED;
            case "CANCELLED" -> CANCELLED;
            default -> SCHEDULED;
        };
    }
}
