package edu.uni.airportsim.domain;

public abstract class AbstractBoardAircraftRole implements CrewRole, BoardAircraftCapable {
    private final String code;
    private final String displayName;
    private final boolean requiredForFlightDeck;

    protected AbstractBoardAircraftRole(String code, String displayName, boolean requiredForFlightDeck) {
        this.code = BaseEntity.requireText(code, "code");
        this.displayName = BaseEntity.requireText(displayName, "displayName");
        this.requiredForFlightDeck = requiredForFlightDeck;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public boolean requiredForFlightDeck() {
        return requiredForFlightDeck;
    }

    @Override
    public OperationResult boardAircraft(StaffMember staffMember, Aircraft aircraft, Flight flight) {
        if (staffMember == null) {
            return OperationResult.rejected("Staff member is required");
        }
        if (!staffMember.hasRole(code)) {
            return OperationResult.rejected(staffMember.getDisplayName() + " does not have role " + displayName);
        }
        if (aircraft == null || flight == null) {
            return OperationResult.rejected("Aircraft and flight are required");
        }
        return OperationResult.approved(staffMember.getPerson().getFullName() + " boarded aircraft as " + displayName);
    }
}
