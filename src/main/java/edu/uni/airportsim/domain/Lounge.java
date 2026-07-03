package edu.uni.airportsim.domain;

public class Lounge extends BaseEntity {
    private final int capacity;

    public Lounge(String id, String name, int capacity) {
        super(id, name);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }
}
