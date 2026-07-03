package edu.uni.airportsim.domain;

public class CheckInDesk extends BaseEntity {
    private StaffMember assignedAgent;
    private boolean open;

    public CheckInDesk(String id, String name) {
        super(id, name);
    }

    public StaffMember getAssignedAgent() {
        return assignedAgent;
    }

    public void assignAgent(StaffMember assignedAgent) {
        this.assignedAgent = assignedAgent;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
