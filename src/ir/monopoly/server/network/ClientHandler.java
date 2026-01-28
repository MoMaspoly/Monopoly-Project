package ir.monopoly.server.network;

import ir.monopoly.server.game.GameState;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;
import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final int playerId;
    private final GameServer server;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, int playerId, GameServer server) {
        this.socket = socket;
        this.playerId = playerId;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String welcome = "{\"type\":\"CONNECTED\",\"playerId\":" + playerId + ",\"message\":\"Welcome to Monopoly!\"}";
            sendMessage(welcome);

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Command from Player " + playerId + ": " + line);

                try {

                    String[] parts = line.trim().split("\\s+", 2);
                    String commandType = parts[0].toUpperCase();
                    String extra = parts.length > 1 ? parts[1] : "";

                    String response = server.getGameController().handleCommand(commandType, playerId, extra);

                    if (response != null && !response.isEmpty()) {
                        sendMessage(response);
                    }

                } catch (Exception e) {
                    sendMessage("{\"type\":\"ERROR\",\"message\":\"Invalid command format or logic error.\"}");
                }
            }
        } catch (IOException e) {
            handleDisconnect();
        } finally {
            closeResources();
        }
    }

    private void handleDisconnect() {
        System.out.println("Player " + playerId + " disconnected.");
        server.removeClient(this);

        try {
            GameState gs = server.getGameState();
            if (gs != null) {
                Player p = gs.getPlayerById(playerId);
                if (p != null) {
                    p.setStatus(PlayerStatus.BANKRUPT);

                    String msg = "{\"type\":\"PLAYER_LEFT\",\"playerId\":" + playerId +
                            ",\"playerName\":\"" + p.getName() +
                            "\",\"message\":\"Player disconnected and left the game.\"}";
                    server.broadcast(msg);
                }
            }
        } catch (Exception e) {
            System.err.println("Error during disconnect cleanup: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    private void closeResources() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public int getPlayerId() {
        return playerId;
    }
}
