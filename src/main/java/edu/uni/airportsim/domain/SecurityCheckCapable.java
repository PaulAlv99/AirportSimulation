package edu.uni.airportsim.domain;

public interface SecurityCheckCapable {
    OperationResult inspect(Passenger passenger);
}
