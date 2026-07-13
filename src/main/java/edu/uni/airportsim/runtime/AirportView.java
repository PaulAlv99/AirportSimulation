package edu.uni.airportsim.runtime;

public record AirportView(
        String code,
        String name,
        String city,
        String country,
        long runways
) {
}
