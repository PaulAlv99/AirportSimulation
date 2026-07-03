package edu.uni.airportsim.domain;

public class SecurityOfficerRole implements AirportRole, SecurityCheckCapable {
    public static final String CODE = "SECURITY_OFFICER";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String displayName() {
        return "Security Officer";
    }

    @Override
    public OperationResult inspect(Passenger passenger) {
        if (passenger == null) {
            return OperationResult.rejected("Passenger is required");
        }
        return OperationResult.approved("Passenger passed security inspection");
    }
}
