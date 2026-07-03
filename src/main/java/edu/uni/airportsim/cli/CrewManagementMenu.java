package edu.uni.airportsim.cli;

import java.util.Scanner;

public class CrewManagementMenu {
    private final Scanner scanner;

    public CrewManagementMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void show() {
        System.out.println("Crew management scaffold:");
        System.out.println("- assign pilot");
        System.out.println("- assign co-pilot");
        System.out.println("- assign flight attendants");
        System.out.println("Press ENTER to return.");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
