package edu.uni.airportsim.domain;

public class Gate extends BaseEntity {
    private GateState state = GateState.AVAILABLE;

    public Gate(String id, String name) {
        super(id, name);
    }

    public GateState getState() {
        return state;
    }

    public void setState(GateState state) {
        this.state = state;
    }
}
