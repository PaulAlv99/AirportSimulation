package edu.uni.airportsim.domain;

public final class VehicleState {
    public static final VehicleState AVAILABLE = new VehicleState("AVAILABLE", "Available", true);
    public static final VehicleState ASSIGNED = new VehicleState("ASSIGNED", "Assigned", true);
    public static final VehicleState OPERATING = new VehicleState("OPERATING", "Operating", false);
    public static final VehicleState MAINTENANCE = new VehicleState("MAINTENANCE", "Maintenance", false);

    private final String code;
    private final String label;
    private final boolean assignable;

    public VehicleState(String code, String label, boolean assignable) {
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
}
