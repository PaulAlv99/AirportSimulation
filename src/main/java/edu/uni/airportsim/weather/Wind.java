package edu.uni.airportsim.weather;

public record Wind(double speedKmh, double gustKmh, int directionDegrees) {
    public Wind {
        if (speedKmh < 0 || gustKmh < 0) {
            throw new IllegalArgumentException("wind speed values must not be negative");
        }
        directionDegrees = Math.floorMod(directionDegrees, 360);
    }

    public String compassDirection() {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) Math.round(directionDegrees / 45.0) % directions.length;
        return directions[index];
    }

    public double crosswindKmh(int runwayHeadingDegrees) {
        double angle = Math.toRadians(relativeAngle(runwayHeadingDegrees));
        return Math.abs(speedKmh * Math.sin(angle));
    }

    public double headwindKmh(int runwayHeadingDegrees) {
        double angle = Math.toRadians(relativeAngle(runwayHeadingDegrees));
        return speedKmh * Math.cos(angle);
    }

    private int relativeAngle(int runwayHeadingDegrees) {
        int diff = Math.floorMod(directionDegrees - runwayHeadingDegrees + 180, 360) - 180;
        return Math.abs(diff);
    }
}
