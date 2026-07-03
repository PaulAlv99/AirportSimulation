package edu.uni.airportsim.domain;

public interface CheckInCapable {
    OperationResult checkIn(Ticket ticket, CheckInDesk desk);
}
