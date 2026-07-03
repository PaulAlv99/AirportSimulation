package edu.uni.airportsim.domain;

public class CheckInAgentRole implements AirportRole, CheckInCapable {
    public static final String CODE = "CHECK_IN_AGENT";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String displayName() {
        return "Check-In Agent";
    }

    @Override
    public OperationResult checkIn(Ticket ticket, CheckInDesk desk) {
        if (ticket == null || desk == null || !desk.isOpen()) {
            return OperationResult.rejected("Ticket and open check-in desk are required");
        }
        return OperationResult.approved("Passenger checked in");
    }
}
