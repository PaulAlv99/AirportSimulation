package edu.uni.airportsim.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Passenger extends Person {
    private final List<Baggage> baggage = new ArrayList<>();

    public Passenger(String id, String fullName, Document document) {
        super(id, fullName, document);
    }

    public List<Baggage> getBaggage() {
        return Collections.unmodifiableList(baggage);
    }

    public void addBaggage(Baggage item) {
        baggage.add(item);
    }
}
