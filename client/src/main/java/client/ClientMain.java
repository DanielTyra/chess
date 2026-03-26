package client;

public class ClientMain {
    public static void main(String[] args) {
        var repl = new Repl("http://localhost:8080");
        repl.run();
    }
}