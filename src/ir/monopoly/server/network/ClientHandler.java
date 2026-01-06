package ir.monopoly.server.network;

import ir.monopoly.server.game.GameState;
import ir.monopoly.server.player.PlayerStatus; // اگر خطا داد، یعنی فایل Mahna نیست
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
                    sendMessage("{\"type\":\"ERROR\",\"message\":\"Invalid command format\"}");
                }
            }
        } catch (IOException e) {
            handleDisconnect();
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleDisconnect() {
        System.out.println("Player " + playerId + " disconnected.");
        server.removeClient(this);

        // مدیریت قطع اتصال با استفاده از لاجیک مهنا
        try {
            GameState gs = server.getGameState();
            if (gs != null && gs.getPlayerById(playerId) != null) {
                gs.getPlayerById(playerId).setStatus(PlayerStatus.BANKRUPT);

                String name = gs.getPlayerById(playerId).getName();
                String msg = "{\"type\":\"PLAYER_LEFT\",\"playerId\":" + playerId +
                        ",\"playerName\":\"" + name + "\",\"message\":\"Player disconnected.\"}";
                server.broadcast(msg);
            }
        } catch (Exception e) {
            System.out.println("Error handling disconnect: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    public int getPlayerId() {
        return playerId;
    }
}
