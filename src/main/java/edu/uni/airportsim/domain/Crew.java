package edu.uni.airportsim.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Crew extends BaseEntity {
    private final List<CrewAssignment> assignments = new ArrayList<>();

    public Crew(String id, String name) {
        super(id, name);
    }

    public List<CrewAssignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    public void assign(CrewAssignment assignment) {
        assignments.add(assignment);
    }

    public boolean hasAssignedRole(String roleCode) {
        return assignments.stream().anyMatch(assignment -> assignment.role().code().equals(roleCode));
    }

    public List<StaffMember> getMembers() {
        return assignments.stream().map(CrewAssignment::staffMember).toList();
    }

    public boolean isReadyForFlight() {
        return hasAssignedRole(PilotRole.CODE) && hasAssignedRole(CoPilotRole.CODE);
    }
}
