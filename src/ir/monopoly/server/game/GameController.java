package ir.monopoly.server.game;

import ir.monopoly.server.board.Tile;
import ir.monopoly.server.board.TileType;
import ir.monopoly.server.network.GameServer;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;
import ir.monopoly.server.property.Property;

public class GameController {
    private final GameState gameState;
    private final GameServer server;

    public GameController(GameState gameState, GameServer server) {
        this.gameState = gameState;
        this.server = server;
    }

    public synchronized String handleCommand(String type, int pId, String extra) {
        Player player = gameState.getTurnManager().getCurrentPlayer();

        // فقط بازیکن فعلی اجازه دستور دادن دارد (بجز قبول کردن تجارت)
        if (player.getPlayerId() != pId && !type.equals("ACCEPT_TRADE")) {
            return "{\"type\":\"ERROR\",\"message\":\"Wait for your turn!\"}";
        }

        switch (type) {
            case "ROLL": return handleRoll(player);
            case "BUY": return handleBuy(player);
            case "TRADE": return handleTradeProposal(player, extra);
            case "ACCEPT_TRADE": return handleAcceptTrade(pId);
            case "END_TURN": return handleEndTurn(player);
            case "GET_TOP_K": return "{\"type\":\"SHOW_CARD\",\"text\":\"" + LeaderboardManager.getTopKReport(gameState) + "\"}";
            case "UNDO": gameState.getUndoManager().undo(); syncGameState(); return null;
            case "REDO": gameState.getUndoManager().redo(); syncGameState(); return null;
            default: return "{\"type\":\"ERROR\",\"message\":\"Unknown command\"}";
        }
    }

    private String handleRoll(Player player) {
        Dice dice = new Dice();
        dice.roll();
        int total = dice.getSum();

        // منطق زندان
        if (player.getStatus() == PlayerStatus.IN_JAIL) {
            if (dice.isDoubles()) {
                player.releaseFromJail();
                server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"Doubles! " + player.getName() + " is FREE!\"}");
            } else {
                player.incrementJailTurns();
                if (player.getJailTurns() >= 3) {
                    player.changeBalance(-50);
                    player.releaseFromJail();
                    server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"3rd turn! " + player.getName() + " paid $50 fine and is free.\"}");
                } else {
                    server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"" + player.getName() + " failed to roll doubles.\"}");
                    return null;
                }
            }
        }

        // حرکت مهره بر اساس تاس
        int oldPos = player.getCurrentPosition();
        int newPos = (oldPos + total) % 40;
        player.setCurrentPosition(newPos);

        server.broadcast("{\"type\":\"ROLL_UPDATE\",\"playerId\":" + player.getPlayerId() + ",\"currentPosition\":" + newPos + "}");

        // تحلیل مقصد
        Tile tile = gameState.getBoard().getTileAt(newPos);
        TileResolver.resolveTile(tile, gameState);

        // همگام‌سازی وضعیت (از جمله جابه‌جایی‌های کارت)
        syncGameState();
        return null;
    }

    private void syncGameState() {
        Player currentP = gameState.getTurnManager().getCurrentPlayer();
        String event = gameState.getLastEvent();

        // اگر کارت مهره را جابه‌جا کرده باشد (مثل Advance to GO)، دستور حرکت مجدد صادر می‌شود
        if (event.contains("GO") || event.contains("Jail") || event.contains("spaces")) {
            server.broadcast("{\"type\":\"ROLL_UPDATE\",\"playerId\":" + currentP.getPlayerId() + ",\"currentPosition\":" + currentP.getCurrentPosition() + "}");
        }

        // بروزرسانی پول همه
        for (Player p : gameState.getPlayers()) {
            server.broadcast("{\"type\":\"PLAYER_STATS\",\"playerId\":" + p.getPlayerId() + ",\"balance\":" + p.getBalance() + "}");
        }

        // نمایش کارت یا پیام اکشن
        if (event.contains("ACTION_") || event.contains("CARD_DRAWN")) {
            String cleanMsg = event.contains(":") ? event.split(":", 2)[1] : event;
            server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"" + cleanMsg + "\"}");
        }
    }

    private String handleBuy(Player p) {
        Tile tile = gameState.getBoard().getTileAt(p.getCurrentPosition());
        if (tile.getTileType() == TileType.PROPERTY) {
            Property prop = (Property) tile.getTileData();
            if (PropertyService.buyProperty(p, prop, gameState)) {
                syncGameState();
                return null;
            }
        }
        return "{\"type\":\"ERROR\",\"message\":\"Cannot buy.\"}";
    }

    private String handleEndTurn(Player p) {
        gameState.getTurnManager().passTurn();
        int nextId = gameState.getTurnManager().getCurrentPlayer().getPlayerId();
        server.broadcast("{\"type\":\"TURN_UPDATE\",\"currentPlayer\":" + nextId + "}");
        return null;
    }

    private String handleTradeProposal(Player s, String e) {
        String[] parts = e.split(" ");
        int targetId = Integer.parseInt(parts[0]);
        server.sendToPlayer(targetId, "{\"type\":\"TRADE_REQUEST\",\"from\":" + s.getPlayerId() + ",\"cash\":" + parts[1] + "}");
        return null;
    }

    private String handleAcceptTrade(int pId) {
        TradeManager.acceptTrade(gameState);
        syncGameState();
        return null;
    }
}
