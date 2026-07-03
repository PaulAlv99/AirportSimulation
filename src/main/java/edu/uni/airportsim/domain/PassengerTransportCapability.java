package edu.uni.airportsim.domain;

public class PassengerTransportCapability implements VehicleCapability {
    @Override
    public String code() {
        return "PASSENGER_TRANSPORT";
    }

    @Override
    public String displayName() {
        return "Passenger Transport";
    }
}
