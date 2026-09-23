package com.brian.rentaltracker.transport;

import java.util.Scanner;

public class ConsoleIO {

    private final Scanner scanner;

    public ConsoleIO(Scanner scanner) {
        this.scanner = scanner;
    }

    public void print(String message) {
        System.out.println(message);
    }

    public void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    public String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    public int promptInt(String message) {
        while (true) {
            String raw = prompt(message);
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                print("Please enter a whole number.");
            }
        }
    }

    public double promptDouble(String message) {
        while (true) {
            String raw = prompt(message);
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException e) {
                print("Please enter a number.");
            }
        }
    }
}
