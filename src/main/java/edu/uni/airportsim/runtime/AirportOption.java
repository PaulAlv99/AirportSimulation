package edu.uni.airportsim.runtime;

public record AirportOption(
        String code,
        String ident,
        String name,
        String city,
        String country,
        String type,
        Double latitude,
        Double longitude,
        long runways
) {
}
