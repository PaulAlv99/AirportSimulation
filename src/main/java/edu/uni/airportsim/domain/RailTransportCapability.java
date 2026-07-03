package edu.uni.airportsim.domain;

public class RailTransportCapability implements VehicleCapability {
    @Override
    public String code() {
        return "RAIL_TRANSPORT";
    }

    @Override
    public String displayName() {
        return "Rail Transport";
    }
}
