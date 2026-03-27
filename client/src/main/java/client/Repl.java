package client;

import java.util.Locale;
import java.util.Scanner;

public class Repl {
    private final String serverUrl;
    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade serverFacade;

    private boolean running = true;
    private ClientState state = ClientState.LOGGED_OUT;
    private String authToken = null;
    private String username = null;

    public Repl(String serverUrl) {
        this.serverUrl = serverUrl;
        this.serverFacade = new ServerFacade(serverUrl);
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
            case LOGGED_OUT -> evalLoggedOut(tokens);
            case LOGGED_IN -> evalLoggedIn(tokens);
            case GAMEPLAY -> evalGameplay(tokens);
        };
    }

    // ---------------- LOGGED OUT ----------------
    private ClientResponse evalLoggedOut(String[] tokens) {
        String command = tokens[0];

        return switch (command) {
            case "help" -> ClientResponse.success(helpText());
            case "quit", "exit" -> quit();

            case "register" -> handleRegister(tokens);
            case "login" -> handleLogin(tokens);

            default -> ClientResponse.error("Unknown command. Type 'help' to see available commands.");
        };
    }

    private ClientResponse handleRegister(String[] tokens) {
        if (tokens.length < 4) {
            return ClientResponse.error("Usage: register <username> <password> <email>");
        }

        try {
            var result = serverFacade.register(tokens[1], tokens[2], tokens[3]);

            authToken = result.authToken();
            username = result.username();
            state = ClientState.LOGGED_IN;

            return ClientResponse.success("Registered and logged in as " + username);
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    private ClientResponse handleLogin(String[] tokens) {
        if (tokens.length < 3) {
            return ClientResponse.error("Usage: login <username> <password>");
        }

        try {
            var result = serverFacade.login(tokens[1], tokens[2]);

            authToken = result.authToken();
            username = result.username();
            state = ClientState.LOGGED_IN;

            return ClientResponse.success("Logged in as " + username);
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    // ---------------- LOGGED IN ----------------
    private ClientResponse evalLoggedIn(String[] tokens) {
        String command = tokens[0];

        return switch (command) {
            case "help" -> ClientResponse.success(helpText());

            case "logout" -> {
                try {
                    serverFacade.logout(authToken);
                    authToken = null;
                    username = null;
                    state = ClientState.LOGGED_OUT;
                    yield ClientResponse.success("Logged out.");
                } catch (ResponseException e) {
                    yield ClientResponse.error(e.getMessage());
                }
            }

            case "quit", "exit" -> quit();

            default -> ClientResponse.error("Unknown command. Type 'help' to see available commands.");
        };
    }

    // ---------------- GAMEPLAY ----------------
    private ClientResponse evalGameplay(String[] tokens) {
        String command = tokens[0];

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
                      help
                      login <username> <password>
                      register <username> <password> <email>
                      quit
                    """;
            case LOGGED_IN -> """
                    Postlogin commands:
                      help
                      logout
                      create <name>
                      list
                      join <id> [WHITE|BLACK]
                      observe <id>
                      quit
                    """;
            case GAMEPLAY -> """
                    Gameplay commands:
                      help
                      redraw
                      leave
                      move <from> <to>
                      resign
                      highlight <square>
                      quit
                    """;
        };
    }
}