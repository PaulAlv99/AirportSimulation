package edu.uni.airportsim.domain;

public class Baggage extends BaseEntity {
    private final String passengerId;
    private final String ticketId;
    private final double weightKg;
    private BaggageState state = BaggageState.REGISTERED;

    public Baggage(String id, String passengerId, String ticketId, double weightKg) {
        super(id, "Baggage " + id);
        this.passengerId = requireText(passengerId, "passengerId");
        this.ticketId = requireText(ticketId, "ticketId");
        this.weightKg = weightKg;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public BaggageState getState() {
        return state;
    }

    public void setState(BaggageState state) {
        this.state = state;
    }
}
