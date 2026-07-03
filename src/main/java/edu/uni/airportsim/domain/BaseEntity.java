package edu.uni.airportsim.domain;

import java.util.Objects;

public abstract class BaseEntity implements Identifiable, Manageable {
    private final String id;
    private String name;

    protected BaseEntity(String id, String name) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "name");
    }

    @Override
    public String getDisplayName() {
        return name + " [" + id + "]";
    }

    protected static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BaseEntity that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
