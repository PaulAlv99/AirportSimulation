package edu.uni.airportsim.domain;

public record Address(String city, String country) {
    public Address {
        city = city == null ? "" : city;
        country = country == null ? "" : country;
    }
}
