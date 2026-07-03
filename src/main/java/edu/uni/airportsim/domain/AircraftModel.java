package edu.uni.airportsim.domain;

public record AircraftModel(String code, String manufacturer, String modelName, int seatCapacity, boolean cargoCapable) {
    public static final AircraftModel NARROW_BODY = new AircraftModel("NARROW_BODY", "Generic", "Narrow Body", 180, false);
    public static final AircraftModel WIDE_BODY = new AircraftModel("WIDE_BODY", "Generic", "Wide Body", 320, false);
    public static final AircraftModel REGIONAL = new AircraftModel("REGIONAL", "Generic", "Regional", 90, false);
    public static final AircraftModel CARGO = new AircraftModel("CARGO", "Generic", "Cargo", 0, true);

    public AircraftModel {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        manufacturer = manufacturer == null ? "" : manufacturer;
        modelName = modelName == null || modelName.isBlank() ? code : modelName;
        if (seatCapacity < 0) {
            throw new IllegalArgumentException("seatCapacity must not be negative");
        }
    }
}
