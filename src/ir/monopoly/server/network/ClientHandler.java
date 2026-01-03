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

            // پیام خوش‌آمدگویی
            sendMessage("{\"type\":\"CONNECTED\",\"playerId\":" + playerId + ",\"message\":\"Welcome Player " + playerId + "\"}");

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Command from Player " + playerId + ": " + line);

                // 1. اجرای دستور در منطق بازی (کد مهنا)
                String response = server.getGameController().handleCommand(line.toUpperCase(), playerId, "");

                // 2. ساخت پاسخ جیسون با داده‌های واقعی
                String jsonResponse = formatToJson(response, line.toUpperCase());

                // 3. ارسال پاسخ به خود بازیکن
                sendMessage(jsonResponse);

                // 4. خبر دادن به بقیه (مثلاً فلانی حرکت کرد)
                server.broadcast(formatTurnUpdate());
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
        GameState gs = server.getGameState();
        // چک می‌کنیم بازی نول نباشد
        if (gs != null) {
            Player p = gs.getPlayerById(playerId);
            if (p != null) p.setStatus(PlayerStatus.BANKRUPT);

            // خبر به بقیه
            server.broadcast("{\"type\":\"PLAYER_DISCONNECTED\",\"playerId\":" + playerId + ",\"message\":\"Player disconnected.\"}");

            // اگر نوبت او بود، نوبت رد شود
            if (gs.getTurnManager().getCurrentPlayer().getPlayerId() == playerId) {
                gs.getTurnManager().passTurn();
                server.broadcast(formatTurnUpdate());
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // --- بخش اصلاح شده (FIXED) ---
    private String formatToJson(String response, String command) {
        // دسترسی به وضعیت واقعی بازیکن از طریق سرور -> گیم‌استیت
        Player player = server.getGameState().getPlayerById(playerId);

        // فرار از کاراکترهای خاص برای جلوگیری از خرابی JSON
        String safeResponse = response.replace("\"", "'");

        if (command.startsWith("ROLL")) {
            // خواندن موقعیت و پول واقعی از بازیکن
            int realPosition = player.getCurrentPosition();
            int realBalance = player.getBalance();

            return "{" +
                    "\"type\":\"ROLL_UPDATE\"," +
                    "\"playerId\":" + playerId + "," +
                    "\"currentPosition\":" + realPosition + "," +
                    "\"balance\":" + realBalance + "," +
                    "\"message\":\"" + safeResponse + "\"" +
                    "}";
        }

        if (command.startsWith("BUY")) {
            int realBalance = player.getBalance();
            // اینجا لیست دارایی‌ها را هم می‌توان فرستاد اما فعلاً بالانس کافی است
            return "{" +
                    "\"type\":\"BUY_UPDATE\"," +
                    "\"playerId\":" + playerId + "," +
                    "\"balance\":" + realBalance + "," +
                    "\"message\":\"" + safeResponse + "\"" +
                    "}";
        }

        if (command.equals("END_TURN")) {
            return "{\"type\":\"TURN_END\",\"playerId\":" + playerId + ",\"message\":\"Turn ended.\"}";
        }

        // پیام پیش‌فرض
        return "{\"type\":\"GENERAL\",\"playerId\":" + playerId + ",\"message\":\"" + safeResponse + "\"}";
    }

    private String formatTurnUpdate() {
        int currentPlayerId = server.getGameState().getTurnManager().getCurrentPlayer().getPlayerId();
        return "{\"type\":\"TURN_UPDATE\",\"currentPlayer\":" + currentPlayerId + "}";
    }
}
