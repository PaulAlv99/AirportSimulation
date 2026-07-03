package edu.uni.airportsim.simulation;

public final class SimulationLifecycleState {
    public static final SimulationLifecycleState CREATED = new SimulationLifecycleState("CREATED", "Created");
    public static final SimulationLifecycleState LOADED = new SimulationLifecycleState("LOADED", "Loaded");
    public static final SimulationLifecycleState RUNNING = new SimulationLifecycleState("RUNNING", "Running");
    public static final SimulationLifecycleState PAUSED = new SimulationLifecycleState("PAUSED", "Paused");
    public static final SimulationLifecycleState STOPPED = new SimulationLifecycleState("STOPPED", "Stopped");

    private final String code;
    private final String label;

    public SimulationLifecycleState(String code, String label) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        this.code = code;
        this.label = label == null || label.isBlank() ? code : label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static SimulationLifecycleState fromCode(String code) {
        return switch (code) {
            case "LOADED" -> LOADED;
            case "RUNNING" -> RUNNING;
            case "PAUSED" -> PAUSED;
            case "STOPPED" -> STOPPED;
            default -> CREATED;
        };
    }
}
