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

        // Only current player can act (except accepting a trade)
        if (player.getPlayerId() != pId && !type.equals("ACCEPT_TRADE")) {
            return "{\"type\":\"ERROR\",\"message\":\"Wait your turn!\"}";
        }

        switch (type) {
            case "ROLL" -> { return handleRoll(player); }
            case "BUY" -> { return handleBuy(player); }
            case "TRADE" -> { return handleTradeProposal(player, extra); }
            case "ACCEPT_TRADE" -> { return handleAcceptTrade(pId); }
            case "END_TURN" -> { return handleEndTurn(player); }
            case "GET_TOP_K" -> { return "{\"type\":\"SHOW_CARD\",\"text\":\"" + LeaderboardManager.getTopKReport(gameState) + "\"}"; }
            default -> { return "{\"type\":\"ERROR\",\"message\":\"Unknown Command\"}"; }
        }
    }

    private String handleRoll(Player player) {
        Dice dice = new Dice();
        dice.roll();
        int total = dice.getSum();

        if (player.getStatus() == PlayerStatus.IN_JAIL) {
            if (dice.isDoubles()) {
                player.releaseFromJail();
                server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"Doubles! " + player.getName() + " is FREE!\"}");
            } else {
                player.incrementJailTurns();
                if (player.getJailTurns() >= 3) {
                    player.changeBalance(-50);
                    player.releaseFromJail();
                    server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"Forced release! " + player.getName() + " paid $50 fine.\"}");
                } else {
                    server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"" + player.getName() + " is still in Jail.\"}");
                    return null;
                }
            }
        }

        int newPos = (player.getCurrentPosition() + total) % 40;
        player.setCurrentPosition(newPos);
        server.broadcast("{\"type\":\"ROLL_UPDATE\",\"playerId\":" + player.getPlayerId() + ",\"currentPosition\":" + newPos + "}");

        TileResolver.resolveTile(gameState.getBoard().getTileAt(newPos), gameState);
        syncGameState();
        return null;
    }

    private String handleEndTurn(Player p) {
        Tile tile = gameState.getBoard().getTileAt(p.getCurrentPosition());
        if (tile.getTileType() == TileType.PROPERTY) {
            Property prop = (Property) tile.getTileData();
            if (prop.getOwnerId() == null) {
                processAuction(prop);
            }
        }
        gameState.getTurnManager().passTurn();
        server.broadcast("{\"type\":\"TURN_UPDATE\",\"currentPlayer\":" + gameState.getTurnManager().getCurrentPlayer().getPlayerId() + "}");
        return null;
    }

    private void processAuction(Property prop) {
        Player winner = null;
        int maxWealth = -1;
        for (Player p : gameState.getPlayers()) {
            if (p.getBalance() > maxWealth) {
                maxWealth = p.getBalance();
                winner = p;
            }
        }
        if (winner != null && winner.getBalance() >= prop.getPurchasePrice() * 0.8) {
            int bid = (int)(prop.getPurchasePrice() * 0.8);
            winner.changeBalance(-bid);
            winner.addProperty(prop);
            server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"AUCTION: " + winner.getName() + " won " + prop.getName() + " for $" + bid + "\"}");
        }
    }

    private String handleTradeProposal(Player sender, String extra) {
        String[] parts = extra.split(" ");
        int target = Integer.parseInt(parts[0]);
        int cash = Integer.parseInt(parts[1]);
        server.sendToPlayer(target, "{\"type\":\"TRADE_REQUEST\",\"from\":" + sender.getPlayerId() + ",\"cash\":" + cash + "}");
        return "{\"type\":\"INFO\",\"message\":\"Trade proposal sent.\"}";
    }

    private String handleAcceptTrade(int playerId) {
        TradeManager.acceptTrade(gameState);
        syncGameState();
        return null;
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
        return "{\"type\":\"ERROR\",\"message\":\"Purchase failed.\"}";
    }

    private void syncGameState() {
        for (Player p : gameState.getPlayers()) {
            server.broadcast("{\"type\":\"PLAYER_STATS\",\"playerId\":" + p.getPlayerId() + ",\"balance\":" + p.getBalance() + "}");
        }
        String event = gameState.getLastEvent();
        if (event.contains("ACTION_")) {
            server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"" + event.split(":", 2)[1] + "\"}");
        }
    }
}
