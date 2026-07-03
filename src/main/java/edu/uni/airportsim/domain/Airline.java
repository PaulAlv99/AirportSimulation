package edu.uni.airportsim.domain;

public class Airline extends BaseEntity {
    private final String code;

    public Airline(String id, String name, String code) {
        super(id, name);
        this.code = requireText(code, "code");
    }

    public String getCode() {
        return code;
    }
}
