package edu.uni.airportsim.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class StaffMember extends BaseEntity {
    private final Person person;
    private final EmployeeProfile employeeProfile;
    private final List<AirportRole> roles = new ArrayList<>();

    public StaffMember(String id, Person person, EmployeeProfile employeeProfile) {
        super(id, person == null ? id : person.getFullName());
        if (person == null) {
            throw new IllegalArgumentException("person must not be null");
        }
        this.person = person;
        this.employeeProfile = employeeProfile;
    }

    public Person getPerson() {
        return person;
    }

    public EmployeeProfile getEmployeeProfile() {
        return employeeProfile;
    }

    public List<AirportRole> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public void assignRole(AirportRole role) {
        if (role == null || hasRole(role.code())) {
            return;
        }
        roles.add(role);
    }

    public boolean hasRole(String roleCode) {
        return roles.stream().anyMatch(role -> role.code().equals(roleCode));
    }

    public <T extends AirportRole> Optional<T> findRole(Class<T> roleType) {
        return roles.stream()
                .filter(roleType::isInstance)
                .map(roleType::cast)
                .findFirst();
    }
}
