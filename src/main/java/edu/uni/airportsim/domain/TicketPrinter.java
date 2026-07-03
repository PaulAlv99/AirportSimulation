package edu.uni.airportsim.domain;

import java.time.LocalDateTime;

public class TicketPrinter {
    public String printTicket(Ticket ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("ticket must not be null");
        }
        return "TICKET %s | PASSENGER %s | FLIGHT %s | SEAT %s".formatted(
                ticket.getId(),
                ticket.getPassenger().getFullName(),
                ticket.getFlight().getFlightNumber(),
                ticket.getSeatNumber()
        );
    }

    public BoardingPass printBoardingPass(Ticket ticket, Gate gate, LocalDateTime boardingTime) {
        BoardingPass boardingPass = new BoardingPass(
                "BP-" + ticket.getId(),
                ticket.getId(),
                gate == null ? "" : gate.getName(),
                boardingTime
        );
        ticket.setBoardingPass(boardingPass);
        return boardingPass;
    }
}
