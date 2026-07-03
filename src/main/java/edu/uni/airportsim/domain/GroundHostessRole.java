package edu.uni.airportsim.domain;

public class GroundHostessRole implements AirportRole {
    public static final String CODE = "GROUND_HOSTESS";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String displayName() {
        return "Ground Hostess";
    }
}
