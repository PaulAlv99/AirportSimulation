package edu.uni.airportsim.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroundVehicle extends BaseEntity {
    private final VehicleProfile profile;
    private final List<VehicleCapability> capabilities = new ArrayList<>();
    private VehicleState state = VehicleState.AVAILABLE;

    public GroundVehicle(String id, String name, VehicleProfile profile) {
        super(id, name);
        this.profile = profile;
    }

    public VehicleProfile getProfile() {
        return profile;
    }

    public List<VehicleCapability> getCapabilities() {
        return Collections.unmodifiableList(capabilities);
    }

    public void addCapability(VehicleCapability capability) {
        if (capability == null || hasCapability(capability.code())) {
            return;
        }
        capabilities.add(capability);
    }

    public boolean hasCapability(String capabilityCode) {
        return capabilities.stream().anyMatch(capability -> capability.code().equals(capabilityCode));
    }

    public VehicleState getState() {
        return state;
    }

    public void setState(VehicleState state) {
        this.state = state;
    }
}
