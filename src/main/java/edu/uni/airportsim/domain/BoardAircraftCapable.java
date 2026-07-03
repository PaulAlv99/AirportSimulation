package edu.uni.airportsim.domain;

public interface BoardAircraftCapable {
    OperationResult boardAircraft(StaffMember staffMember, Aircraft aircraft, Flight flight);
}
