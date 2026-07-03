package edu.uni.airportsim.domain;

public class CheckInService {
    public OperationResult checkIn(Ticket ticket, CheckInDesk desk) {
        if (desk == null || desk.getAssignedAgent() == null) {
            return OperationResult.rejected("Assigned check-in staff member is required");
        }
        return desk.getAssignedAgent()
                .findRole(CheckInAgentRole.class)
                .map(role -> role.checkIn(ticket, desk))
                .orElseGet(() -> OperationResult.rejected("Assigned staff member cannot perform check-in"));
    }
}
