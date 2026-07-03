package edu.uni.airportsim.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ControlTower extends BaseEntity {
    private final List<String> activeClearances = new ArrayList<>();

    public ControlTower(String id, String name) {
        super(id, name);
    }

    public List<String> getActiveClearances() {
        return Collections.unmodifiableList(activeClearances);
    }

    public void addClearance(String clearance) {
        activeClearances.add(requireText(clearance, "clearance"));
    }
}
