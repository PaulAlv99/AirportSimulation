package edu.uni.airportsim.domain;

public class CoPilotRole extends AbstractBoardAircraftRole {
    public static final String CODE = "CO_PILOT";

    public CoPilotRole() {
        super(CODE, "Co-Pilot", true);
    }
}
