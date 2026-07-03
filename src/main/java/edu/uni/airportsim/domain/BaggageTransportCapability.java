package edu.uni.airportsim.domain;

public class BaggageTransportCapability implements VehicleCapability {
    @Override
    public String code() {
        return "BAGGAGE_TRANSPORT";
    }

    @Override
    public String displayName() {
        return "Baggage Transport";
    }
}
