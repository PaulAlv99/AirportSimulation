package edu.uni.airportsim.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleCompositionTest {
    @Test
    void personCanPlayPilotRoleWithoutSubclassing() {
        Person person = new Person("PER-1", "Maria Costa", null);
        StaffMember staffMember = new StaffMember("STF-1", person, new EmployeeProfile("EMP-1", "Flight Deck"));
        PilotRole pilotRole = new PilotRole();
        staffMember.assignRole(pilotRole);

        Aircraft aircraft = new Aircraft("AC-1", "A320", "CS-TST", AircraftModel.NARROW_BODY);
        Flight flight = new Flight(
                "FLT-1",
                "TP101",
                new Airline("AIR-TP", "TAP Air Portugal", "TP"),
                new Route("LIS", "OPO"),
                new FlightSchedule(LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 9, 0))
        );

        OperationResult result = pilotRole.boardAircraft(staffMember, aircraft, flight);

        assertTrue(result.success());
        assertTrue(staffMember.hasRole(PilotRole.CODE));
    }

    @Test
    void crewUsesRoleAssignments() {
        StaffMember pilot = new StaffMember("STF-1", new Person("P1", "Pilot One", null), new EmployeeProfile("E1", "Flight Deck"));
        StaffMember coPilot = new StaffMember("STF-2", new Person("P2", "Co Pilot", null), new EmployeeProfile("E2", "Flight Deck"));
        pilot.assignRole(new PilotRole());
        coPilot.assignRole(new CoPilotRole());

        Crew crew = new Crew("CREW-1", "Morning Crew");
        crew.assign(new CrewAssignment(pilot, new PilotRole()));
        crew.assign(new CrewAssignment(coPilot, new CoPilotRole()));

        assertTrue(crew.isReadyForFlight());
        assertEquals(2, crew.getMembers().size());
    }
}
