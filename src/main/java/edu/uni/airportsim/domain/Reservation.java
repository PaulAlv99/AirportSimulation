package edu.uni.airportsim.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Reservation extends BaseEntity {
    private final PassengerGroup passengerGroup;
    private final List<Ticket> tickets = new ArrayList<>();

    public Reservation(String id, String name, PassengerGroup passengerGroup) {
        super(id, name);
        this.passengerGroup = passengerGroup;
    }

    public PassengerGroup getPassengerGroup() {
        return passengerGroup;
    }

    public List<Ticket> getTickets() {
        return Collections.unmodifiableList(tickets);
    }

    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
    }
}
