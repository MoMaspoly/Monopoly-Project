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
    private boolean awaitingBuyDecision = false;
    private Property propertyForSale = null;

    public GameController(GameState gameState, GameServer server) {
        this.gameState = gameState;
        this.server = server;
    }

    public synchronized String handleCommand(String type, int pId, String extra) {
        Player player = gameState.getPlayerById(pId);
        if (player == null) {
            return "{\"type\":\"ERROR\",\"message\":\"Player not found!\"}";
        }

        // 🔴 رد درخواست اگر بازیکن ورشکسته باشد
        if (player.getStatus() == PlayerStatus.BANKRUPT) {
            return "{\"type\":\"ERROR\",\"message\":\"You are bankrupt and cannot act!\"}";
        }

        // بررسی مزایده فعال
        if (gameState.isAuctionActive()) {
            return handleAuctionCommand(type, pId, extra);
        }

        // بررسی ترید pending
        if (type.equals("ACCEPT_TRADE") || type.equals("REJECT_TRADE")) {
            return handleTradeCommand(type, pId, extra);
        }

        // بازیکن فعلی
        Player currentPlayer = gameState.getTurnManager().getCurrentPlayer();

        // فقط بازیکن فعلی اجازه دستور دادن دارد (بجز مزایده و ترید)
        if (currentPlayer.getPlayerId() != pId && !type.equals("ACCEPT_TRADE") && !type.equals("REJECT_TRADE")) {
            return "{\"type\":\"ERROR\",\"message\":\"Wait for your turn!\"}";
        }

        switch (type) {
            case "ROLL": return handleRoll(player);
            case "BUY": return handleBuy(player);
            case "PASS": return handlePass(player);
            case "BID": return handleBid(player, extra);
            case "TRADE": return handleTradeProposal(player, extra);
            case "ACCEPT_TRADE": return handleAcceptTrade(pId);
            case "REJECT_TRADE": return handleRejectTrade(pId);
            case "MORTGAGE": return handleMortgage(player, extra);
            case "UNMORTGAGE": return handleUnmortgage(player, extra);
            case "BUILD": return handleBuild(player, extra);
            case "END_TURN": return handleEndTurn(player);
            case "GET_TOP_K":
                String report = LeaderboardManager.getTopKReport(gameState);
                report = escapeJson(report);
                return "{\"type\":\"SHOW_CARD\",\"text\":\"" + report + "\"}";
            case "UNDO":
                boolean undone = gameState.getUndoManager().undo();
                if (undone) {
                    syncGameStateAfterUndoRedo();
                }
                return null;
            case "REDO":
                boolean redone = gameState.getUndoManager().redo();
                if (redone) {
                    syncGameStateAfterUndoRedo();
                }
                return null;
            default: return "{\"type\":\"ERROR\",\"message\":\"Unknown command\"}";
        }
    }

    private String handleAuctionCommand(String type, int pId, String extra) {
        AuctionManager auction = gameState.getAuctionManager();
        Player player = gameState.getPlayerById(pId);

        if (auction == null || player == null) {
            return "{\"type\":\"ERROR\",\"message\":\"No active auction!\"}";
        }

        switch (type) {
            case "BID":
                try {
                    int amount = Integer.parseInt(extra.trim());
                    if (auction.placeBid(player, amount)) {
                        // Broadcast auction update
                        broadcastAuctionStatus();
                        gameState.addEvent("AUCTION_BID: " + player.getName() + " bid $" + amount);
                        return null;
                    } else {
                        return "{\"type\":\"ERROR\",\"message\":\"Invalid bid! Must be higher than current bid and you must have enough money.\"}";
                    }
                } catch (NumberFormatException e) {
                    return "{\"type\":\"ERROR\",\"message\":\"Invalid bid amount!\"}";
                }

            case "PASS":
                if (auction.passBid(player)) {
                    broadcastAuctionStatus();
                    gameState.addEvent("AUCTION_PASS: " + player.getName() + " passed");

                    if (auction.isFinished()) {
                        endAuction();
                    }
                    return null;
                }
                return "{\"type\":\"ERROR\",\"message\":\"Cannot pass!\"}";

            default:
                return "{\"type\":\"ERROR\",\"message\":\"Invalid command during auction!\"}";
        }
    }

    private void broadcastAuctionStatus() {
        AuctionManager auction = gameState.getAuctionManager();
        if (auction != null) {
            String status = auction.getAuctionStatus();
            Property property = auction.getProperty();
            Player currentBidder = auction.getCurrentBidder();

            String json = "{\"type\":\"AUCTION_UPDATE\",\"property\":\"" + escapeJson(property.getName()) +
                    "\",\"currentBid\":" + auction.getCurrentHighestBid() +
                    ",\"currentBidder\":" + (currentBidder != null ? currentBidder.getPlayerId() : -1) +
                    ",\"status\":\"" + escapeJson(status) + "\"}";
            server.broadcast(json);
        }
    }

    private void endAuction() {
        AuctionManager auction = gameState.getAuctionManager();
        if (auction != null) {
            Property property = auction.getProperty();
            Player winner = auction.getCurrentWinner();
            int winningBid = auction.getCurrentHighestBid();

            if (winner != null) {
                // Record transaction for Top-K
                gameState.getTransactionGraph().recordTransaction(winner.getPlayerId(), 0, winningBid); // 0 = bank

                // Add event
                gameState.addEvent("AUCTION_WON: " + winner.getName() + " won " + property.getName() + " for $" + winningBid);
            }

            // Broadcast auction end
            server.broadcast("{\"type\":\"AUCTION_END\",\"winner\":" + (winner != null ? winner.getPlayerId() : -1) +
                    ",\"property\":\"" + escapeJson(property.getName()) + "\",\"amount\":" + winningBid + "}");

            gameState.endAuction();
            syncGameState();
        }
    }

    private String handleRoll(Player player) {
        // اگر مزایده فعال است، نمی‌توان Roll زد
        if (gameState.isAuctionActive()) {
            return "{\"type\":\"ERROR\",\"message\":\"Finish the auction first!\"}";
        }

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
                    int oldBalance = player.getBalance();
                    player.changeBalance(-50);
                    gameState.getUndoManager().recordAction(new GameAction(
                            GameAction.ActionType.MONEY_CHANGE,
                            player.getPlayerId(),
                            oldBalance,
                            player.getBalance(),
                            -1
                    ));
                    player.releaseFromJail();
                    server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"3rd turn! " + player.getName() + " paid $50 fine and is free.\"}");
                } else {
                    server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"" + player.getName() + " failed to roll doubles.\"}");
                    return null;
                }
            }
        }

        int oldPosition = player.getCurrentPosition();
        int newPos = (oldPosition + total) % 40;
        player.setCurrentPosition(newPos);

        // ثبت action برای حرکت
        gameState.getUndoManager().recordAction(new GameAction(
                GameAction.ActionType.MOVEMENT,
                player.getPlayerId(),
                oldPosition,
                newPos,
                -1
        ));

        server.broadcast("{\"type\":\"ROLL_UPDATE\",\"playerId\":" + player.getPlayerId() + ",\"currentPosition\":" + newPos + "}");

        // تحلیل مقصد
        Tile tile = gameState.getBoard().getTileAt(newPos);
        TileResolver.resolveTile(tile, gameState);

        // اگر روی ملک بدون مالک فرود آمد
        if (tile.getTileType() == TileType.PROPERTY) {
            Property prop = (Property) tile.getTileData();
            if (prop.getOwnerId() == null) {
                propertyForSale = prop;
                awaitingBuyDecision = true;

                // به بازیکن گزینه خرید بده
                String message = "You landed on " + prop.getName() + ". Price: $" + prop.getPurchasePrice() +
                        ". Your balance: $" + player.getBalance() +
                        ". Do you want to buy?";
                server.sendToPlayer(player.getPlayerId(),
                        "{\"type\":\"BUY_OFFER\",\"property\":\"" + escapeJson(prop.getName()) +
                                "\",\"price\":" + prop.getPurchasePrice() +
                                ",\"message\":\"" + escapeJson(message) + "\"}");
            }
        }

        syncGameState();
        return null;
    }

    private String handleBuy(Player player) {
        if (!awaitingBuyDecision || propertyForSale == null) {
            return "{\"type\":\"ERROR\",\"message\":\"No property to buy!\"}";
        }

        if (player.getBalance() >= propertyForSale.getPurchasePrice()) {
            int oldBalance = player.getBalance();

            if (PropertyService.buyProperty(player, propertyForSale, gameState)) {
                gameState.getUndoManager().recordAction(new GameAction(
                        GameAction.ActionType.PROPERTY_PURCHASE,
                        player.getPlayerId(),
                        oldBalance,
                        player.getBalance(),
                        propertyForSale.getPropertyId()
                ));

                awaitingBuyDecision = false;
                propertyForSale = null;
                syncGameState();
                return null;
            }
        }

        // اگر پول کافی ندارد یا خرید نکرد، مزایده شروع شود
        return handlePass(player);
    }

    private String handlePass(Player player) {
        if (awaitingBuyDecision && propertyForSale != null) {
            // شروع مزایده
            gameState.startAuction(propertyForSale);
            awaitingBuyDecision = false;

            // Broadcast شروع مزایده
            broadcastAuctionStatus();
            server.broadcast("{\"type\":\"AUCTION_START\",\"property\":\"" + escapeJson(propertyForSale.getName()) +
                    "\",\"minBid\":10,\"message\":\"Auction started for " + escapeJson(propertyForSale.getName()) + "\"}");

            propertyForSale = null;
            return null;
        }

        return "{\"type\":\"ERROR\",\"message\":\"Nothing to pass!\"}";
    }

    private String handleBid(Player player, String amountStr) {
        // این فقط برای زمانی است که مستقیماً BID فرستاده شود (نه در مزایده)
        return "{\"type\":\"ERROR\",\"message\":\"No active auction! Use PASS to start auction.\"}";
    }

    /**
     * Special sync for undo/redo operations
     */
    private void syncGameStateAfterUndoRedo() {
        String lastEvent = gameState.getLastEvent();

        // Update ALL players' stats
        for (Player p : gameState.getPlayers()) {
            server.broadcast("{\"type\":\"PLAYER_STATS\",\"playerId\":" + p.getPlayerId() + ",\"balance\":" + p.getBalance() + "}");
            sendPlayerProperties(p);
        }

        // Update ALL players' positions
        for (Player p : gameState.getPlayers()) {
            server.broadcast("{\"type\":\"ROLL_UPDATE\",\"playerId\":" + p.getPlayerId() + ",\"currentPosition\":" + p.getCurrentPosition() + "}");
        }

        // Send event message
        if (lastEvent.contains("UNDO:") || lastEvent.contains("REDO:")) {
            String cleanMsg = lastEvent.contains(":") ? lastEvent.split(":", 2)[1] : lastEvent;
            cleanMsg = escapeJson(cleanMsg);
            server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"" + cleanMsg + "\"}");
        }

        // Send to log
        String safeEvent = escapeJson(lastEvent);
        server.broadcast("{\"type\":\"EVENT_LOG\",\"message\":\"" + safeEvent + "\"}");
    }

    private void syncGameState() {
        Player currentP = gameState.getTurnManager().getCurrentPlayer();
        String event = gameState.getLastEvent();

        // 🔴 اطلاع ورشکستگی
        for (Player p : gameState.getPlayers()) {
            if (p.getStatus() == PlayerStatus.BANKRUPT) {
                server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"" +
                        escapeJson("💀 " + p.getName() + " went BANKRUPT!") + "\"}");
                server.broadcast("{\"type\":\"EVENT_LOG\",\"message\":\"" +
                        escapeJson(p.getName() + " is bankrupt and out of the game.") + "\"}");
            }
        }

        // حرکت بازیکن
        if (event.contains("GO") || event.contains("Jail") || event.contains("spaces")) {
            server.broadcast("{\"type\":\"ROLL_UPDATE\",\"playerId\":" + currentP.getPlayerId() + ",\"currentPosition\":" + currentP.getCurrentPosition() + "}");
        }

        // بروزرسانی وضعیت همه بازیکنان
        for (Player p : gameState.getPlayers()) {
            server.broadcast("{\"type\":\"PLAYER_STATS\",\"playerId\":" + p.getPlayerId() + ",\"balance\":" + p.getBalance() + "}");

            // ارسال وضعیت زندان
            String status = p.getStatus().toString();
            int jailTurns = p.getJailTurns();
            server.sendToPlayer(p.getPlayerId(),
                    "{\"type\":\"PLAYER_STATUS\",\"playerId\":" + p.getPlayerId() +
                            ",\"status\":\"" + status +
                            "\",\"jailTurns\":" + jailTurns + "}");

            sendPlayerProperties(p);
        }

        // نمایش کارت/رویداد
        if (event.contains("ACTION_") || event.contains("CARD_DRAWN") ||
                event.contains("AUCTION_") || event.contains("JAIL")) {
            String cleanMsg = event.contains(":") ? event.split(":", 2)[1] : event;
            cleanMsg = escapeJson(cleanMsg);
            server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"" + cleanMsg + "\"}");
        }

        // ثبت در لاگ
        if (!event.isEmpty() && !event.equals("Game Started")) {
            String safeEvent = escapeJson(event);
            server.broadcast("{\"type\":\"EVENT_LOG\",\"message\":\"" + safeEvent + "\"}");
        }
    }

    private void sendPlayerProperties(Player player) {
        StringBuilder propertiesList = new StringBuilder();

        player.getOwnedProperties().forEach(property -> {
            if (propertiesList.length() > 0) propertiesList.append("|");
            propertiesList.append(property.getName())
                    .append(",").append(property.getColorGroup())
                    .append(",").append(property.getHouseCount())
                    .append(",").append(property.hasHotel())
                    .append(",").append(property.isMortgaged())
                    .append(",").append(property.getPurchasePrice());
        });

        String propertiesStr = escapeJson(player.getPropertiesString());

        server.sendToPlayer(player.getPlayerId(),
                "{\"type\":\"PROPERTY_LIST\",\"playerId\":" + player.getPlayerId() +
                        ",\"properties\":\"" + propertiesStr + "\"}");
    }

    private String handleEndTurn(Player p) {
        if (gameState.isAuctionActive()) {
            return "{\"type\":\"ERROR\",\"message\":\"Finish the auction first!\"}";
        }

        gameState.getTurnManager().passTurn();
        int nextId = gameState.getTurnManager().getCurrentPlayer().getPlayerId();
        server.broadcast("{\"type\":\"TURN_UPDATE\",\"currentPlayer\":" + nextId + "}");
        return null;
    }

    private String handleTradeProposal(Player s, String e) {
        String[] parts = e.split(" ");
        int targetId = Integer.parseInt(parts[0]);
        int cashAmount = Integer.parseInt(parts[1]);

        server.sendToPlayer(targetId,
                "{\"type\":\"TRADE_REQUEST\",\"from\":" + s.getPlayerId() +
                        ",\"fromName\":\"" + escapeJson(s.getName()) +
                        "\",\"cash\":" + cashAmount +
                        ",\"message\":\"" + escapeJson(s.getName() + " offers $" + cashAmount + " for a property") + "\"}");
        return null;
    }

    private String handleAcceptTrade(int pId) {
        TradeManager.acceptTrade(gameState);
        syncGameState();
        return null;
    }

    private String handleRejectTrade(int pId) {
        TradeManager.rejectTrade(gameState);
        gameState.addEvent("TRADE_REJECTED: Trade was rejected.");
        syncGameState();
        return null;
    }

    private String handleTradeCommand(String type, int pId, String extra) {
        if (type.equals("ACCEPT_TRADE")) {
            return handleAcceptTrade(pId);
        } else if (type.equals("REJECT_TRADE")) {
            return handleRejectTrade(pId);
        }
        return "{\"type\":\"ERROR\",\"message\":\"Invalid trade command!\"}";
    }

    // متد کمکی برای escape کردن JSON
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String handleMortgage(Player player, String propertyIdStr) {
        try {
            int propertyId = Integer.parseInt(propertyIdStr);
            Property property = gameState.getPropertyById(propertyId);
            if (property == null) {
                return "{\"type\":\"ERROR\",\"message\":\"Property not found!\"}";
            }

            String result = MortgageManager.mortgageProperty(player, property, gameState);
            if (result.equals("SUCCESS")) {
                syncGameState();
                return null;
            } else {
                return "{\"type\":\"ERROR\",\"message\":\"" + escapeJson(result) + "\"}";
            }
        } catch (NumberFormatException e) {
            return "{\"type\":\"ERROR\",\"message\":\"Invalid property ID!\"}";
        }
    }

    private String handleUnmortgage(Player player, String propertyIdStr) {
        try {
            int propertyId = Integer.parseInt(propertyIdStr);
            Property property = gameState.getPropertyById(propertyId);
            if (property == null) {
                return "{\"type\":\"ERROR\",\"message\":\"Property not found!\"}";
            }

            String result = MortgageManager.unmortgageProperty(player, property, gameState);
            if (result.equals("SUCCESS")) {
                syncGameState();
                return null;
            } else {
                return "{\"type\":\"ERROR\",\"message\":\"" + escapeJson(result) + "\"}";
            }
        } catch (NumberFormatException e) {
            return "{\"type\":\"ERROR\",\"message\":\"Invalid property ID!\"}";
        }
    }

    private String handleBuild(Player player, String extra) {
        try {
            int propertyId = Integer.parseInt(extra.trim());
            Property property = gameState.getPropertyById(propertyId);
            if (property == null) {
                return "{\"type\":\"ERROR\",\"message\":\"Property not found!\"}";
            }

            String result = PropertyService.buildOnProperty(player, property, gameState);
            if (result.equals("SUCCESS")) {
                syncGameState();
                return null;
            } else {
                return "{\"type\":\"ERROR\",\"message\":\"" + escapeJson(result) + "\"}";
            }
        } catch (NumberFormatException e) {
            return "{\"type\":\"ERROR\",\"message\":\"Invalid property ID!\"}";
        }
    }
}