package edu.uni.airportsim.domain;

public record Route(String originAirportCode, String destinationAirportCode) {
    public Route {
        if (originAirportCode == null || originAirportCode.isBlank()) {
            throw new IllegalArgumentException("originAirportCode must not be blank");
        }
        if (destinationAirportCode == null || destinationAirportCode.isBlank()) {
            throw new IllegalArgumentException("destinationAirportCode must not be blank");
        }
    }
}
