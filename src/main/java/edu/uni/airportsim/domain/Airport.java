package edu.uni.airportsim.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Airport extends BaseEntity {
    private final String code;
    private final Address address;
    private final List<Terminal> terminals = new ArrayList<>();
    private final List<Runway> runways = new ArrayList<>();
    private final List<ParkingArea> parkingAreas = new ArrayList<>();
    private ControlTower controlTower;

    public Airport(String id, String name, String code, Address address) {
        super(id, name);
        this.code = requireText(code, "code");
        this.address = address;
    }

    public String getCode() {
        return code;
    }

    public Address getAddress() {
        return address;
    }

    public List<Terminal> getTerminals() {
        return Collections.unmodifiableList(terminals);
    }

    public List<Runway> getRunways() {
        return Collections.unmodifiableList(runways);
    }

    public List<ParkingArea> getParkingAreas() {
        return Collections.unmodifiableList(parkingAreas);
    }

    public ControlTower getControlTower() {
        return controlTower;
    }

    public void setControlTower(ControlTower controlTower) {
        this.controlTower = controlTower;
    }

    public void addTerminal(Terminal terminal) {
        terminals.add(terminal);
    }

    public void addRunway(Runway runway) {
        runways.add(runway);
    }

    public void addParkingArea(ParkingArea parkingArea) {
        parkingAreas.add(parkingArea);
    }
}
