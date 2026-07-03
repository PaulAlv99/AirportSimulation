package edu.uni.airportsim.domain;

public class FuelOperation extends GroundOperation {
    private final Aircraft aircraft;
    private final double liters;

    public FuelOperation(String id, String name, GroundVehicle vehicle, Aircraft aircraft, double liters) {
        super(id, name, vehicle, "Refuel aircraft");
        if (!vehicle.hasCapability("FUELING")) {
            throw new IllegalArgumentException("vehicle must have fueling capability");
        }
        this.aircraft = aircraft;
        this.liters = liters;
    }

    public Aircraft getAircraft() {
        return aircraft;
    }

    public double getLiters() {
        return liters;
    }
}
