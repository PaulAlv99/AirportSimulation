package edu.uni.airportsim.domain;

public final class BaggageState {
    public static final BaggageState REGISTERED = new BaggageState("REGISTERED", "Registered");
    public static final BaggageState SCREENED = new BaggageState("SCREENED", "Screened");
    public static final BaggageState LOADED = new BaggageState("LOADED", "Loaded");
    public static final BaggageState IN_TRANSIT = new BaggageState("IN_TRANSIT", "In Transit");
    public static final BaggageState DELIVERED = new BaggageState("DELIVERED", "Delivered");
    public static final BaggageState LOST = new BaggageState("LOST", "Lost");

    private final String code;
    private final String label;

    public BaggageState(String code, String label) {
        this.code = BaseEntity.requireText(code, "code");
        this.label = BaseEntity.requireText(label, "label");
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static BaggageState fromCode(String code) {
        return switch (code) {
            case "SCREENED" -> SCREENED;
            case "LOADED" -> LOADED;
            case "IN_TRANSIT" -> IN_TRANSIT;
            case "DELIVERED" -> DELIVERED;
            case "LOST" -> LOST;
            default -> REGISTERED;
        };
    }
}
