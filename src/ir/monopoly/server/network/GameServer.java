package ir.monopoly.server.network;

import ir.monopoly.server.game.GameController;
import ir.monopoly.server.game.GameInitializer;
import ir.monopoly.server.game.GameState;
import ir.monopoly.server.player.Player;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServer {

    private static final int PORT = 8080;
    private static final int MAX_PLAYERS = 4;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clientHandlers = new ArrayList<>();

    // لیست بازیکنان منطقی (Logic Players)
    private final List<Player> logicPlayers = new ArrayList<>();

    private GameState gameState;
    private GameController gameController;

    public GameServer() throws IOException {
        serverSocket = new ServerSocket(PORT);
    }

    public void startServer() throws IOException {
        System.out.println("=== Monopoly Server Started ===");
        System.out.println("Waiting for " + MAX_PLAYERS + " players...");

        // مرحله ۱: اتصال بازیکنان
        while (clientHandlers.size() < MAX_PLAYERS) {
            Socket socket = serverSocket.accept();
            int playerId = clientHandlers.size() + 1; // ID شبکه (1 تا 4)

            // ساخت هندلر شبکه
            ClientHandler handler = new ClientHandler(socket, playerId, this);
            clientHandlers.add(handler);
            handler.start();

            // ساخت بازیکن منطقی (برای GameState)
            // عدد 1500 همان Initial Balance است
            logicPlayers.add(new Player(playerId, "Player " + playerId, 1500));


            System.out.println("Player " + playerId + " connected.");
        }

        // مرحله ۲: شروع منطق بازی (وقتی ۴ نفر تکمیل شدند)
        System.out.println("All players connected. Initializing GameState...");

        // استفاده از کد مهنا برای ساخت بازی
        gameState = GameInitializer.initializeGame(logicPlayers);
        gameController = new GameController(gameState, this);

        // مرحله ۳: اعلام شروع به همه
        broadcast("{\"type\":\"GAME_START\",\"message\":\"Game Initialized!\"}");

        // اعلام نوبت نفر اول (چون GameState بازیکنان را شافل می‌کند، باید ببینیم نوبت کیست)
        int firstPlayerId = gameState.getTurnManager().getCurrentPlayer().getPlayerId();
        broadcast("{\"type\":\"TURN_UPDATE\",\"currentPlayer\":" + firstPlayerId + "}");
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clientHandlers) {
            if (client.isAlive()) client.sendMessage(message);
        }
    }

    public void sendToPlayer(int targetPlayerId, String message) {
        for (ClientHandler client : clientHandlers) {
            if (client.getPlayerId() == targetPlayerId) {
                client.sendMessage(message);
                return;
            }
        }
    }

    public synchronized void removeClient(ClientHandler handler) {
        clientHandlers.remove(handler);
    }

    public GameController getGameController() {
        return gameController; // حالا ممکن است null باشد اگر بازی هنوز شروع نشده
    }

    public GameState getGameState() {
        return gameState;
    }

    public static void main(String[] args) {
        try {
            new GameServer().startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
