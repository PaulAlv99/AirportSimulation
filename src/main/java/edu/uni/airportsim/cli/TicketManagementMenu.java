package edu.uni.airportsim.cli;

import java.util.Scanner;

public class TicketManagementMenu {
    private final Scanner scanner;

    public TicketManagementMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void show() {
        System.out.println("Ticket management scaffold:");
        System.out.println("- print ticket");
        System.out.println("- print boarding pass");
        System.out.println("- view baggage status");
        System.out.println("Press ENTER to return.");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
