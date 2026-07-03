package edu.uni.airportsim.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PassengerGroup extends BaseEntity {
    private final List<Passenger> passengers = new ArrayList<>();

    public PassengerGroup(String id, String name) {
        super(id, name);
    }

    public List<Passenger> getPassengers() {
        return Collections.unmodifiableList(passengers);
    }

    public void addPassenger(Passenger passenger) {
        passengers.add(passenger);
    }
}
