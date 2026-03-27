package client;

import chess.ChessGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Repl {
    private final String serverUrl;
    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade serverFacade;
    private final ChessBoardRenderer boardRenderer = new ChessBoardRenderer();

    private boolean running = true;
    private ClientState state = ClientState.LOGGED_OUT;
    private String authToken = null;
    private String username = null;
    private List<GameData> lastGameList = new ArrayList<>();

    private Integer currentGameID = null;
    private ChessGame.TeamColor currentPerspective = ChessGame.TeamColor.WHITE;
    private boolean observing = false;

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
        String command = tokens[0].toLowerCase();

        return switch (state) {
            case LOGGED_OUT -> evalLoggedOut(tokens);
            case LOGGED_IN -> evalLoggedIn(tokens);
            case GAMEPLAY -> evalGameplay(tokens);
        };
    }

    private ClientResponse evalLoggedOut(String[] tokens) {
        return switch (tokens[0]) {
            case "help" -> ClientResponse.success(helpText());
            case "quit", "exit" -> quit();
            case "register" -> handleRegister(tokens);
            case "login" -> handleLogin(tokens);
            default -> ClientResponse.error("Unknown command.");
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
            return ClientResponse.success("Registered and logged in.");
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
            return ClientResponse.success("Logged in.");
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    private ClientResponse evalLoggedIn(String[] tokens) {
        return switch (tokens[0]) {
            case "help" -> ClientResponse.success(helpText());

            case "logout" -> {
                try {
                    serverFacade.logout(authToken);
                    authToken = null;
                    username = null;
                    state = ClientState.LOGGED_OUT;
                    currentGameID = null;
                    observing = false;
                    yield ClientResponse.success("Logged out.");
                } catch (ResponseException e) {
                    yield ClientResponse.error(e.getMessage());
                }
            }

            case "create" -> handleCreate(tokens);
            case "list" -> handleList();
            case "join" -> handleJoin(tokens);
            case "observe" -> handleObserve(tokens);
            case "quit", "exit" -> quit();

            default -> ClientResponse.error("Unknown command.");
        };
    }

    private ClientResponse handleCreate(String[] tokens) {
        if (tokens.length < 2) {
            return ClientResponse.error("Usage: create <gameName>");
        }

        try {
            var result = serverFacade.createGame(authToken, tokens[1]);
            return ClientResponse.success("Created game with ID: " + result.gameID());
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    private ClientResponse handleList() {
        try {
            var result = serverFacade.listGames(authToken);
            lastGameList = result.games();

            if (lastGameList == null || lastGameList.isEmpty()) {
                return ClientResponse.success("No games available.");
            }

            StringBuilder output = new StringBuilder();
            for (int i = 0; i < lastGameList.size(); i++) {
                var g = lastGameList.get(i);
                output.append(String.format(
                        "%d: %s (White: %s, Black: %s)%n",
                        i + 1,
                        g.gameName(),
                        g.whiteUsername(),
                        g.blackUsername()
                ));
            }

            return ClientResponse.success(output.toString());
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    private ClientResponse handleJoin(String[] tokens) {
        if (tokens.length < 3) {
            return ClientResponse.error("Usage: join <number> <WHITE|BLACK>");
        }

        try {
            int index = Integer.parseInt(tokens[1]) - 1;

            if (index < 0 || index >= lastGameList.size()) {
                return ClientResponse.error("Invalid game number.");
            }

            String colorText = tokens[2].toUpperCase();
            if (!colorText.equals("WHITE") && !colorText.equals("BLACK")) {
                return ClientResponse.error("Color must be WHITE or BLACK.");
            }

            Integer gameID = lastGameList.get(index).gameID();
            serverFacade.joinGame(authToken, gameID, colorText);

            currentGameID = gameID;
            currentPerspective = colorText.equals("BLACK")
                    ? ChessGame.TeamColor.BLACK
                    : ChessGame.TeamColor.WHITE;
            observing = false;
            state = ClientState.GAMEPLAY;

            return ClientResponse.success("Joined game.\n" + boardRenderer.drawBoard(currentPerspective));
        } catch (NumberFormatException e) {
            return ClientResponse.error("Game number must be a number.");
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    private ClientResponse handleObserve(String[] tokens) {
        if (tokens.length < 2) {
            return ClientResponse.error("Usage: observe <number>");
        }

        try {
            int index = Integer.parseInt(tokens[1]) - 1;

            if (index < 0 || index >= lastGameList.size()) {
                return ClientResponse.error("Invalid game number.");
            }

            Integer gameID = lastGameList.get(index).gameID();
            serverFacade.observeGame(authToken, gameID);

            currentGameID = gameID;
            currentPerspective = ChessGame.TeamColor.WHITE;
            observing = true;
            state = ClientState.GAMEPLAY;

            return ClientResponse.success("Observing game.\n" + boardRenderer.drawBoard(currentPerspective));
        } catch (NumberFormatException e) {
            return ClientResponse.error("Game number must be a number.");
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    private ClientResponse evalGameplay(String[] tokens) {
        return switch (tokens[0]) {
            case "help" -> ClientResponse.success(helpText());

            case "redraw" -> ClientResponse.success(boardRenderer.drawBoard(currentPerspective));

            case "leave" -> {
                currentGameID = null;
                observing = false;
                state = ClientState.LOGGED_IN;
                yield ClientResponse.success("Left game.");
            }

            case "quit", "exit" -> quit();
            default -> ClientResponse.error("Unknown command.");
        };
    }

    private ClientResponse quit() {
        running = false;
        return ClientResponse.success("Exiting...");
    }

    private String helpText() {
        return switch (state) {
            case LOGGED_OUT -> """
                    help
                    login <u> <p>
                    register <u> <p> <e>
                    quit
                    """;
            case LOGGED_IN -> """
                    help
                    logout
                    create <name>
                    list
                    join <num> <WHITE|BLACK>
                    observe <num>
                    quit
                    """;
            case GAMEPLAY -> """
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