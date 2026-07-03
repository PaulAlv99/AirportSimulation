package edu.uni.airportsim.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Terminal extends BaseEntity {
    private final List<Gate> gates = new ArrayList<>();
    private final List<Lounge> lounges = new ArrayList<>();
    private final List<CheckInDesk> checkInDesks = new ArrayList<>();

    public Terminal(String id, String name) {
        super(id, name);
    }

    public List<Gate> getGates() {
        return Collections.unmodifiableList(gates);
    }

    public List<Lounge> getLounges() {
        return Collections.unmodifiableList(lounges);
    }

    public List<CheckInDesk> getCheckInDesks() {
        return Collections.unmodifiableList(checkInDesks);
    }

    public void addGate(Gate gate) {
        gates.add(gate);
    }

    public void addLounge(Lounge lounge) {
        lounges.add(lounge);
    }

    public void addCheckInDesk(CheckInDesk checkInDesk) {
        checkInDesks.add(checkInDesk);
    }
}
