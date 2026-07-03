package edu.uni.airportsim.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketPrinterTest {
    @Test
    void printsTicketAndBoardingPass() {
        Passenger passenger = new Passenger("PAX-1", "Ana Silva", null);
        Flight flight = new Flight(
                "FLT-TP100",
                "TP100",
                new Airline("AIR-TP", "TAP Air Portugal", "TP"),
                new Route("LIS", "OPO"),
                new FlightSchedule(LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 9, 0))
        );
        Ticket ticket = new Ticket("TCK-1", passenger, flight, "12A", Money.euros("120"));
        Gate gate = new Gate("G-A1", "Gate A1");

        TicketPrinter printer = new TicketPrinter();
        String output = printer.printTicket(ticket);
        BoardingPass boardingPass = printer.printBoardingPass(ticket, gate, LocalDateTime.of(2026, 1, 1, 7, 30));

        assertTrue(output.contains("Ana Silva"));
        assertTrue(output.contains("TP100"));
        assertEquals("Gate A1", boardingPass.getGateName());
        assertSame(boardingPass, ticket.getBoardingPass());
    }
}
