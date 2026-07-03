package edu.uni.airportsim.domain;

public class ParkingArea extends BaseEntity {
    private final int capacity;
    private int occupiedSpaces;

    public ParkingArea(String id, String name, int capacity) {
        super(id, name);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getOccupiedSpaces() {
        return occupiedSpaces;
    }

    public void setOccupiedSpaces(int occupiedSpaces) {
        if (occupiedSpaces < 0 || occupiedSpaces > capacity) {
            throw new IllegalArgumentException("occupiedSpaces must be between 0 and capacity");
        }
        this.occupiedSpaces = occupiedSpaces;
    }
}
