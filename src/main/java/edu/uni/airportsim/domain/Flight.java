package edu.uni.airportsim.domain;

public class Flight extends BaseEntity {
    private final String flightNumber;
    private final Airline airline;
    private final Route route;
    private final FlightSchedule schedule;
    private Aircraft aircraft;
    private Gate gate;
    private FlightState state = FlightState.SCHEDULED;

    public Flight(String id, String flightNumber, Airline airline, Route route, FlightSchedule schedule) {
        super(id, flightNumber);
        this.flightNumber = requireText(flightNumber, "flightNumber");
        this.airline = airline;
        this.route = route;
        this.schedule = schedule;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public Airline getAirline() {
        return airline;
    }

    public Route getRoute() {
        return route;
    }

    public FlightSchedule getSchedule() {
        return schedule;
    }

    public Aircraft getAircraft() {
        return aircraft;
    }

    public void assignAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
    }

    public Gate getGate() {
        return gate;
    }

    public void assignGate(Gate gate) {
        this.gate = gate;
    }

    public FlightState getState() {
        return state;
    }

    public void transitionTo(FlightState nextState) {
        if (!state.canTransitionTo(nextState) && state != nextState) {
            throw new IllegalStateException("Flight cannot transition from " + state.code() + " to " + nextState.code());
        }
        this.state = nextState;
    }

    public void setState(FlightState state) {
        this.state = state;
    }
}
