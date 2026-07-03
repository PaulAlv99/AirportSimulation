package edu.uni.airportsim.domain;

public class GroundOperation extends BaseEntity {
    private final GroundVehicle vehicle;
    private final String description;

    public GroundOperation(String id, String name, GroundVehicle vehicle, String description) {
        super(id, name);
        this.vehicle = vehicle;
        this.description = description == null ? "" : description;
    }

    public GroundVehicle getVehicle() {
        return vehicle;
    }

    public String getDescription() {
        return description;
    }
}
