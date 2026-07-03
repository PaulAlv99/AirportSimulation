package edu.uni.airportsim.domain;

public class BaggageHandlingService {
    public void updateState(Baggage baggage, BaggageState state) {
        if (baggage == null) {
            throw new IllegalArgumentException("baggage must not be null");
        }
        baggage.setState(state);
    }
}
