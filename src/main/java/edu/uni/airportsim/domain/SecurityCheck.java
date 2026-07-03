package edu.uni.airportsim.domain;

public class SecurityCheck {
    public OperationResult inspect(Passenger passenger, StaffMember officer) {
        if (officer == null) {
            return OperationResult.rejected("Security officer is required");
        }
        return officer.findRole(SecurityOfficerRole.class)
                .map(role -> role.inspect(passenger))
                .orElseGet(() -> OperationResult.rejected("Staff member cannot perform security inspection"));
    }
}
