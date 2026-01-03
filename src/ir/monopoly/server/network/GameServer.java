package ir.monopoly.server.network;

import ir.monopoly.server.game.GameController;
import ir.monopoly.server.game.GameState;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class GameServer {

    private static final int PORT = 8080;
    private static final int MAX_PLAYERS = 4;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();
    private GameState gameState;
    private GameController gameController;

    public GameServer() throws IOException {
        serverSocket = new ServerSocket(PORT);
        // راه‌اندازی برد و بازی
        gameState = GameInitializer.createGame();
        gameController = new GameController(gameState);
    }

    public void startServer() throws IOException {
        System.out.println("=== Monopoly Server Started ===");
        System.out.println("Port: " + PORT);
        System.out.println("Waiting for " + MAX_PLAYERS + " players...");

        while (clients.size() < MAX_PLAYERS) {
            Socket socket = serverSocket.accept();
            int playerId = clients.size() + 1;

            ClientHandler handler = new ClientHandler(socket, playerId, this);
            clients.add(handler);
            handler.start();

            System.out.println("Player " + playerId + " connected.");
        }

        System.out.println("All 4 players connected! Game Started.");
        broadcast("{\"type\":\"GAME_START\",\"message\":\"Game started! Player 1's turn.\"}");

        broadcast("{\"type\":\"TURN_UPDATE\",\"currentPlayer\":1}");
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            if (client != null && client.isAlive()) {
                client.sendMessage(message);
            }
        }
    }

    public GameController getGameController() { return gameController; }
    public GameState getGameState() { return gameState; }

    public static void main(String[] args) {
        try {
            new GameServer().startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
