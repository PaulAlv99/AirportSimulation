package edu.uni.airportsim.domain;

public record EmployeeProfile(String employeeNumber, String department) {
    public EmployeeProfile {
        if (employeeNumber == null || employeeNumber.isBlank()) {
            throw new IllegalArgumentException("employeeNumber must not be blank");
        }
        department = department == null ? "" : department;
    }
}
