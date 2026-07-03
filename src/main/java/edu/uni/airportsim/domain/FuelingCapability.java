package edu.uni.airportsim.domain;

public class FuelingCapability implements VehicleCapability {
    private final double fuelCapacityLiters;

    public FuelingCapability(double fuelCapacityLiters) {
        this.fuelCapacityLiters = fuelCapacityLiters;
    }

    public double getFuelCapacityLiters() {
        return fuelCapacityLiters;
    }

    @Override
    public String code() {
        return "FUELING";
    }

    @Override
    public String displayName() {
        return "Fueling";
    }
}
