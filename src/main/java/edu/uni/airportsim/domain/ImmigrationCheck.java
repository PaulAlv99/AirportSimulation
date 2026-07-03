package edu.uni.airportsim.domain;

public class ImmigrationCheck {
    public OperationResult inspect(Passenger passenger) {
        if (passenger == null || passenger.getDocument() == null) {
            return OperationResult.rejected("Passenger document is required");
        }
        return OperationResult.approved("Passenger passed immigration inspection");
    }
}
