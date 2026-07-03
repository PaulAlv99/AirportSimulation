package edu.uni.airportsim.domain;

public record VehicleProfile(String code, String displayName, int capacity) {
    public static final VehicleProfile AIRPORT_BUS = new VehicleProfile("AIRPORT_BUS", "Airport Bus", 80);
    public static final VehicleProfile AIRPORT_TRAIN = new VehicleProfile("AIRPORT_TRAIN", "Airport Train", 250);
    public static final VehicleProfile FUEL_TRUCK = new VehicleProfile("FUEL_TRUCK", "Fuel Truck", 1);
    public static final VehicleProfile BAGGAGE_TRUCK = new VehicleProfile("BAGGAGE_TRUCK", "Baggage Truck", 1);

    public VehicleProfile {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        displayName = displayName == null || displayName.isBlank() ? code : displayName;
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
    }
}
