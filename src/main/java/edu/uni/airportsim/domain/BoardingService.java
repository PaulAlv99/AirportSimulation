package edu.uni.airportsim.domain;

public class BoardingService {
    public OperationResult board(Ticket ticket, Gate gate) {
        if (ticket == null || gate == null || !gate.getState().isUsableForBoarding()) {
            return OperationResult.rejected("Ticket and usable gate are required");
        }
        return OperationResult.approved("Passenger boarded at " + gate.getName());
    }
}
