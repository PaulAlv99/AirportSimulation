package edu.uni.airportsim.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleCompositionTest {
    @Test
    void groundVehicleUsesCapabilitiesInsteadOfSubclasses() {
        GroundVehicle vehicle = new GroundVehicle("VEH-1", "Fuel Truck 1", VehicleProfile.FUEL_TRUCK);

        vehicle.addCapability(new FuelingCapability(20_000));

        assertTrue(vehicle.hasCapability("FUELING"));
    }
}
