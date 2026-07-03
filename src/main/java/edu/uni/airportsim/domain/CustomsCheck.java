package edu.uni.airportsim.domain;

public class CustomsCheck {
    public OperationResult inspect(Passenger passenger) {
        return passenger == null
                ? OperationResult.rejected("Passenger is required")
                : OperationResult.approved("Passenger passed customs inspection");
    }
}
