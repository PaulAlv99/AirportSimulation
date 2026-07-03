package edu.uni.airportsim.domain;

public class Runway extends BaseEntity {
    private RunwayState state = RunwayState.AVAILABLE;
    private int headingDegrees;

    public Runway(String id, String name) {
        super(id, name);
    }

    public Runway(String id, String name, int headingDegrees) {
        super(id, name);
        this.headingDegrees = headingDegrees;
    }

    public RunwayState getState() {
        return state;
    }

    public void setState(RunwayState state) {
        this.state = state;
    }

    public int getHeadingDegrees() {
        return headingDegrees;
    }

    public void setHeadingDegrees(int headingDegrees) {
        this.headingDegrees = Math.floorMod(headingDegrees, 360);
    }
}
