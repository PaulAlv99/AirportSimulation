package edu.uni.airportsim.cli;

import java.util.Scanner;

public class PassengerManagementMenu {
    private final Scanner scanner;

    public PassengerManagementMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void show() {
        System.out.println("Passenger management scaffold:");
        System.out.println("- manage passengers");
        System.out.println("- manage passenger groups");
        System.out.println("- manage reservations");
        System.out.println("- view check-in/security/customs/immigration status");
        System.out.println("Press ENTER to return.");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
