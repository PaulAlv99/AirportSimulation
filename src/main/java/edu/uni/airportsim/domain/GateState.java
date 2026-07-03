package edu.uni.airportsim.domain;

public final class GateState {
    public static final GateState AVAILABLE = new GateState("AVAILABLE", "Available", true);
    public static final GateState BOARDING = new GateState("BOARDING", "Boarding", true);
    public static final GateState OCCUPIED = new GateState("OCCUPIED", "Occupied", false);
    public static final GateState CLOSED = new GateState("CLOSED", "Closed", false);

    private final String code;
    private final String label;
    private final boolean usableForBoarding;

    public GateState(String code, String label, boolean usableForBoarding) {
        this.code = BaseEntity.requireText(code, "code");
        this.label = BaseEntity.requireText(label, "label");
        this.usableForBoarding = usableForBoarding;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean isUsableForBoarding() {
        return usableForBoarding;
    }

    public static GateState fromCode(String code) {
        return switch (code) {
            case "BOARDING" -> BOARDING;
            case "OCCUPIED" -> OCCUPIED;
            case "CLOSED" -> CLOSED;
            default -> AVAILABLE;
        };
    }
}
