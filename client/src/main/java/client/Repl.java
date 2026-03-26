package client;

import java.util.Locale;
import java.util.Scanner;

public class Repl {
    private final String serverUrl;
    private final Scanner scanner = new Scanner(System.in);
    private boolean running = true;

    public Repl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void run() {
        printWelcome();

        while (running) {
            try {
                printPrompt();
                String line = scanner.nextLine().trim();
                String output = eval(line);
                if (output != null && !output.isBlank()) {
                    System.out.println(output);
                }
            } catch (Exception e) {
                System.out.println("Error: unable to process command.");
            }
        }

        System.out.println("Goodbye!");
    }

    private void printWelcome() {
        System.out.println("♕ 240 Chess Client");
        System.out.println("Connected to " + serverUrl);
        System.out.println("Type 'help' to get started.");
    }

    private void printPrompt() {
        System.out.print("[LOGGED_OUT] >>> ");
    }

    public String eval(String input) {
        if (input == null || input.isBlank()) {
            return helpText();
        }

        String[] tokens = input.toLowerCase(Locale.ROOT).split("\\s+");
        String command = tokens[0];

        return switch (command) {
            case "help" -> helpText();
            case "quit", "exit" -> quit();
            case "login" -> "Login is not wired up yet.";
            case "register" -> "Register is not wired up yet.";
            default -> "Unknown command. Type 'help' to see available commands.";
        };
    }

    private String quit() {
        running = false;
        return "Exiting...";
    }

    private String helpText() {
        return """
                Prelogin commands:
                  help     - Show available commands
                  login    - Sign in to an existing account
                  register - Create a new account
                  quit     - Exit the program
                """;
    }
}