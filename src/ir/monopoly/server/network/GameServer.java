package ir.monopoly.server.network;

import ir.monopoly.server.game.GameController;
import ir.monopoly.server.game.GameInitializer;
import ir.monopoly.server.game.GameState;
import ir.monopoly.server.player.Player;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class GameServer {

    private static final int PORT = 8080;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final List<Player> logicPlayers = new ArrayList<>();
    private GameState gameState;
    private GameController gameController;
    private boolean gameStarted = false;

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("SERVER: Monopoly Server started on port " + PORT);
            System.out.println("SERVER: Waiting for 4 players to connect...");

            while (clients.size() < 4) {
                Socket socket = serverSocket.accept();
                int playerId = clients.size() + 1;

                Player newPlayer = new Player(playerId, "Player " + playerId, 1500);
                logicPlayers.add(newPlayer);

                ClientHandler handler = new ClientHandler(socket, playerId, this);
                clients.add(handler);
                handler.start();

                System.out.println("SERVER: Player " + playerId + " connected. (" + clients.size() + "/4)");
            }

            initializeGameLogic();

        } catch (IOException e) {
            System.err.println("SERVER ERROR: " + e.getMessage());
        }
    }

    private void initializeGameLogic() {
        System.out.println("SERVER: All players joined. Initializing GameState...");

        this.gameState = GameInitializer.initializeGame(logicPlayers);
        this.gameController = new GameController(gameState, this);
        this.gameStarted = true;

        broadcast("{\"type\":\"INFO\",\"message\":\"Game Started! Good luck!\"}");

        int firstId = gameState.getTurnManager().getCurrentPlayer().getPlayerId();
        broadcast("{\"type\":\"TURN_UPDATE\",\"currentPlayer\":" + firstId + "}");

        System.out.println("SERVER: Logic ready. First turn: Player " + firstId);
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void sendToPlayer(int playerId, String message) {
        for (ClientHandler client : clients) {
            if (client.getPlayerId() == playerId) {
                client.sendMessage(message);
                break;
            }
        }
    }

    public synchronized void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    public GameController getGameController() {
        return gameController;
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public static void main(String[] args) {
        new GameServer().startServer();
    }
}
