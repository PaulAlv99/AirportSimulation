package edu.uni.airportsim.domain;

public class FlightAttendantRole extends AbstractBoardAircraftRole {
    public static final String CODE = "FLIGHT_ATTENDANT";

    public FlightAttendantRole() {
        super(CODE, "Flight Attendant", false);
    }
}
