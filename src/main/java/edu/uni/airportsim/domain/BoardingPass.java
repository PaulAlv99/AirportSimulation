package edu.uni.airportsim.domain;

import java.time.LocalDateTime;

public class BoardingPass extends BaseEntity {
    private final String ticketId;
    private final String gateName;
    private final LocalDateTime boardingTime;

    public BoardingPass(String id, String ticketId, String gateName, LocalDateTime boardingTime) {
        super(id, "Boarding Pass " + id);
        this.ticketId = requireText(ticketId, "ticketId");
        this.gateName = gateName == null ? "" : gateName;
        this.boardingTime = boardingTime;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getGateName() {
        return gateName;
    }

    public LocalDateTime getBoardingTime() {
        return boardingTime;
    }
}
