package edu.uni.airportsim.domain;

public record CrewAssignment(StaffMember staffMember, CrewRole role) {
    public CrewAssignment {
        if (staffMember == null) {
            throw new IllegalArgumentException("staffMember must not be null");
        }
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        if (!staffMember.hasRole(role.code())) {
            throw new IllegalArgumentException("staff member does not have role " + role.code());
        }
    }
}
