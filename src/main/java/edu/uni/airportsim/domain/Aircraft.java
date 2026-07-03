package edu.uni.airportsim.domain;

public class Aircraft extends BaseEntity {
    private final String registration;
    private final AircraftModel model;
    private AircraftState state = AircraftState.AVAILABLE;
    private Crew crew;

    public Aircraft(String id, String name, String registration, AircraftModel model) {
        super(id, name);
        this.registration = requireText(registration, "registration");
        this.model = model;
    }

    public String getRegistration() {
        return registration;
    }

    public AircraftModel getModel() {
        return model;
    }

    public AircraftState getState() {
        return state;
    }

    public void setState(AircraftState state) {
        this.state = state;
    }

    public Crew getCrew() {
        return crew;
    }

    public void assignCrew(Crew crew) {
        this.crew = crew;
    }
}
