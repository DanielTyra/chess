package client;

import java.util.Locale;
import java.util.Scanner;

public class Repl {
    private final String serverUrl;
    private final Scanner scanner = new Scanner(System.in);
    private boolean running = true;
    private ClientState state = ClientState.LOGGED_OUT;
    private String authToken = null;
    private String username = null;

    public Repl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void run() {
        printWelcome();

        while (running) {
            try {
                printPrompt();
                String line = scanner.nextLine().trim();
                ClientResponse response = eval(line);
                if (response.message() != null && !response.message().isBlank()) {
                    System.out.println(response.message());
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
        System.out.printf("[%s", state);
        if (username != null) {
            System.out.printf(":%s", username);
        }
        System.out.print("] >>> ");
    }

    public ClientResponse eval(String input) {
        if (input == null || input.isBlank()) {
            return ClientResponse.success(helpText());
        }

        String[] tokens = input.split("\\s+");
        String command = tokens[0].toLowerCase(Locale.ROOT);

        return switch (state) {
            case LOGGED_OUT -> evalLoggedOut(command);
            case LOGGED_IN -> evalLoggedIn(command);
            case GAMEPLAY -> evalGameplay(command);
        };
    }

    private ClientResponse evalLoggedOut(String command) {
        return switch (command) {
            case "help" -> ClientResponse.success(helpText());
            case "quit", "exit" -> quit();
            case "login" -> ClientResponse.success("Login is not wired up yet.");
            case "register" -> ClientResponse.success("Register is not wired up yet.");
            default -> ClientResponse.error("Unknown command. Type 'help' to see available commands.");
        };
    }

    private ClientResponse evalLoggedIn(String command) {
        return switch (command) {
            case "help" -> ClientResponse.success(helpText());
            case "logout" -> {
                authToken = null;
                username = null;
                state = ClientState.LOGGED_OUT;
                yield ClientResponse.success("Logged out.");
            }
            case "quit", "exit" -> quit();
            default -> ClientResponse.error("Unknown command. Type 'help' to see available commands.");
        };
    }

    private ClientResponse evalGameplay(String command) {
        return switch (command) {
            case "help" -> ClientResponse.success(helpText());
            case "leave" -> {
                state = ClientState.LOGGED_IN;
                yield ClientResponse.success("Left gameplay view.");
            }
            case "quit", "exit" -> quit();
            default -> ClientResponse.error("Unknown command. Type 'help' to see available commands.");
        };
    }

    private ClientResponse quit() {
        running = false;
        return ClientResponse.success("Exiting...");
    }

    private String helpText() {
        return switch (state) {
            case LOGGED_OUT -> """
                    Prelogin commands:
                      help     - Show available commands
                      login    - Sign in to an existing account
                      register - Create a new account
                      quit     - Exit the program
                    """;
            case LOGGED_IN -> """
                    Postlogin commands:
                      help      - Show available commands
                      logout    - Sign out
                      create    - Create a game
                      list      - List games
                      join      - Join a game
                      observe   - Observe a game
                      quit      - Exit the program
                    """;
            case GAMEPLAY -> """
                    Gameplay commands:
                      help         - Show available commands
                      redraw       - Redraw the board
                      leave        - Leave the game view
                      move         - Make a move
                      resign       - Resign the game
                      highlight    - Show legal moves for a piece
                      quit         - Exit the program
                    """;
        };
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public ClientState getState() {
        return state;
    }

    public void setState(ClientState state) {
        this.state = state;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}