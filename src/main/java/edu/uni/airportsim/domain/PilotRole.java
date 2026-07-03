package edu.uni.airportsim.domain;

public class PilotRole extends AbstractBoardAircraftRole {
    public static final String CODE = "PILOT";

    public PilotRole() {
        super(CODE, "Pilot", true);
    }
}
