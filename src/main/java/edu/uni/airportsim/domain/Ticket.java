package edu.uni.airportsim.domain;

public class Ticket extends BaseEntity {
    private final Passenger passenger;
    private final Flight flight;
    private final String seatNumber;
    private final Money price;
    private BoardingPass boardingPass;

    public Ticket(String id, Passenger passenger, Flight flight, String seatNumber, Money price) {
        super(id, "Ticket " + id);
        this.passenger = passenger;
        this.flight = flight;
        this.seatNumber = requireText(seatNumber, "seatNumber");
        this.price = price;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public Flight getFlight() {
        return flight;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public Money getPrice() {
        return price;
    }

    public BoardingPass getBoardingPass() {
        return boardingPass;
    }

    public void setBoardingPass(BoardingPass boardingPass) {
        this.boardingPass = boardingPass;
    }
}
