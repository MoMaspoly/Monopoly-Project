package ir.monopoly.server.game;

import ir.monopoly.server.board.Tile;
import ir.monopoly.server.board.TileType;
import ir.monopoly.server.network.GameServer;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;
import ir.monopoly.server.property.Property;

import java.util.Random;

public class GameController {

    private final GameState gameState;
    private final GameServer server;
    private final Random random = new Random();

    public GameController(GameState gameState, GameServer server) {
        this.gameState = gameState;
        this.server = server;
    }

    public synchronized String handleCommand(String commandType, int requestPlayerId, String extra) {
        if (gameState == null) return "{\"type\":\"ERROR\",\"message\":\"Waiting for players...\"}";

        Player currentPlayer = gameState.getTurnManager().getCurrentPlayer();

        // اگر بازیکن ورشکسته است، نباید کاری کند
        if (currentPlayer.getStatus() == PlayerStatus.BANKRUPT && !commandType.equals("GET_STATS")) {
            return "{\"type\":\"ERROR\",\"message\":\"You are bankrupt!\"}";
        }

        // چک نوبت (به جز درخواست آمار)
        if (currentPlayer.getPlayerId() != requestPlayerId && !commandType.equals("GET_STATS")) {
            return "{\"type\":\"ERROR\",\"message\":\"It is NOT your turn!\"}";
        }

        try {
            switch (commandType) {
                case "ROLL":
                    return handleRoll(currentPlayer);
                case "BUY":
                    return handleBuy(currentPlayer);
                case "BUILD_REQUEST":
                    return handleBuild(currentPlayer);
                case "END_TURN":
                    return handleEndTurn();
                case "BID":
                    // اتصال به AuctionManager (ساده)
                    int bidAmount = Integer.parseInt(extra.trim());
                    server.broadcast("{\"type\":\"INFO\",\"message\":\"Player " + requestPlayerId + " bid $" + bidAmount + "\"}");
                    return null;
                case "TRADE":
                    return handleTrade(currentPlayer, extra);
                case "GET_STATS":
                    return "{\"type\":\"INFO\",\"message\":\"Stats requested\"}";
                default:
                    return "{\"type\":\"ERROR\",\"message\":\"Unknown command\"}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"type\":\"ERROR\",\"message\":\"Server Error: " + e.getMessage() + "\"}";
        }
    }

    // --- ۱. منطق تاس و حرکت ---
    private String handleRoll(Player player) {
        // استفاده از کلاس Dice که فرستادی
        Dice dice = new Dice();
        int[] result = dice.roll();
        int dice1 = result[0];
        int dice2 = result[1];
        int total = dice1 + dice2;

        if (dice.isDoubles()) {
            gameState.getTurnManager().registerDoubles();
            if (gameState.getTurnManager().hasThreeConsecutiveDoubles()) {
                sendToJail(player);
                return null;
            }
        }

        int currentPos = player.getCurrentPosition();
        int newPos = (currentPos + total) % 40;

        // عبور از GO (توسط MoveService یا دستی)
        if (newPos < currentPos) {
            GoHandler.handlePassingGo(player);
            server.broadcast("{\"type\":\"INFO\",\"message\":\"Player " + player.getName() + " passed GO and collected $200!\"}");
        }

        player.setCurrentPosition(newPos);

        // ارسال آپدیت حرکت به کلاینت
        String moveMsg = "{\"type\":\"ROLL_UPDATE\",\"playerId\":" + player.getPlayerId() +
                ",\"dice1\":" + dice1 + ",\"dice2\":" + dice2 +
                ",\"currentPosition\":" + newPos +
                ",\"message\":\"rolled " + total + "\"}";
        server.broadcast(moveMsg);

        // بررسی خانه مقصد (اتصال به TileResolver)
        handleTileEffect(player, newPos);

        // ارسال آپدیت پول (چون ممکن است در حرکت پول گرفته باشد)
        sendStatsUpdate(player);

        return null;
    }

    // --- ۲. بررسی اثر خانه (اتصال به Logic) ---
    private void handleTileEffect(Player player, int pos) {
        if (gameState.getBoard() == null) return;
        Tile tile = gameState.getBoard().getTileAt(pos);
        if (tile == null) return;

        // الف) اگر کارت است: باید دستی هندل کنیم تا متن کارت را به کلاینت بفرستیم
        if (tile.getTileType() == TileType.CARD) {
            handleCardTile(player, tile);
        }
        // ب) اگر ملک است و صاحب ندارد: پیشنهاد خرید
        else if (tile.getTileType() == TileType.PROPERTY) {
            Property prop = (Property) tile.getTileData();
            if (prop.getOwnerId() == null) {
                server.sendToPlayer(player.getPlayerId(),
                        "{\"type\":\"INFO\",\"message\":\"For Sale: " + prop.getName() + " ($" + prop.getPurchasePrice() + ")\"}");
            } else {
                // اگر صاحب دارد، TileResolver اجاره را حساب می‌کند
                TileResolver.resolveTile(tile, gameState);
                sendStatsUpdate(player); // آپدیت پول پس از پرداخت اجاره
            }
        }
        // ج) بقیه موارد (مالیات، زندان و...) را TileResolver انجام دهد
        else {
            TileResolver.resolveTile(tile, gameState);
            sendStatsUpdate(player);
        }
    }

    // --- مدیریت اختصاصی کارت‌ها (برای ارسال متن به UI) ---
    private void handleCardTile(Player player, Tile tile) {
        Card card;
        // تشخیص نوع کارت از روی مکان
        int pos = player.getCurrentPosition();
        if (pos == 7 || pos == 22 || pos == 36) {
            card = gameState.getCardDeck().drawChance();
        } else {
            card = gameState.getCardDeck().drawCommunityChest();
        }

        if (card != null) {
            // ۱. نمایش پاپ‌آپ در کلاینت
            server.broadcast("{\"type\":\"SHOW_CARD\",\"text\":\"" + card.getDescription() + "\",\"playerId\":" + player.getPlayerId() + "}");

            // ۲. اجرای منطق کارت (جابجایی یا پول)
            card.execute(player, gameState);

            // ۳. آپدیت وضعیت (چون کارت ممکن است مهره را جابجا کرده باشد یا پول داده باشد)
            sendStatsUpdate(player);
            server.broadcast("{\"type\":\"ROLL_UPDATE\",\"playerId\":" + player.getPlayerId() +
                    ",\"currentPosition\":" + player.getCurrentPosition() + "}");

            // اگر کارت بازیکن را جابجا کرد، باید اثر خانه جدید هم چک شود (مثلا رفت روی مالیات)
            if (player.getCurrentPosition() != pos) {
                handleTileEffect(player, player.getCurrentPosition());
            }
        }
    }

    // --- ۳. خرید ملک (اتصال به PropertyService) ---
    private String handleBuy(Player player) {
        int pos = player.getCurrentPosition();
        if (gameState.getBoard() == null) return "{\"type\":\"ERROR\",\"message\":\"Board error\"}";
        Tile tile = gameState.getBoard().getTileAt(pos);

        if (tile != null && tile.getTileData() instanceof Property) {
            Property prop = (Property) tile.getTileData();

            // استفاده از سرویس خرید شما
            boolean success = PropertyService.buyProperty(player, prop, gameState);

            if (success) {
                sendStatsUpdate(player); // آپدیت پول و لیست املاک در کلاینت
                server.broadcast("{\"type\":\"BUY_UPDATE\",\"playerId\":" + player.getPlayerId() +
                        ",\"success\":true,\"message\":\"Bought " + prop.getName() + "\"}");
                return null;
            } else {
                return "{\"type\":\"ERROR\",\"message\":\"Cannot buy (Insufficient funds or owned)\"}";
            }
        }
        return "{\"type\":\"ERROR\",\"message\":\"Cannot buy here.\"}";
    }

    // --- ۴. ساخت و ساز (اتصال به ConstructionManager) ---
    private String handleBuild(Player player) {
        int pos = player.getCurrentPosition();
        Tile tile = gameState.getBoard().getTileAt(pos);

        if (tile != null && tile.getTileData() instanceof Property) {
            Property prop = (Property) tile.getTileData();

            // استفاده از سرویس ساخت و ساز شما
            String result = ConstructionManager.buildHouse(player, prop, gameState);

            if (result.equals("SUCCESS")) {
                sendStatsUpdate(player);
                int count = prop.hasHotel() ? 5 : prop.getHouseCount();
                server.broadcast("{\"type\":\"HOUSE_BUILT\",\"tileId\":" + prop.getPropertyId() +
                        ",\"count\":" + count +
                        ",\"message\":\"Construction successful on " + prop.getName() + "\"}");
                return null;
            } else {
                return "{\"type\":\"ERROR\",\"message\":\"" + result + "\"}";
            }
        }
        return "{\"type\":\"ERROR\",\"message\":\"Cannot build here.\"}";
    }

    // --- ۵. مدیریت ترید (اتصال به TradeManager) ---
    private String handleTrade(Player player, String extra) {
        // فرمت ساده ورودی از کلاینت: "TargetID Amount" (مثلا: 2 100)
        try {
            String[] parts = extra.split(" ");
            int targetId = Integer.parseInt(parts[0]);
            int amount = Integer.parseInt(parts[1]); // پولی که پیشنهاد میدهیم

            // ساخت آبجکت ترید (فعلا بدون ملک، فقط پول)
            TradeOffer offer = new TradeOffer(player.getPlayerId(), targetId, amount, 0, new Property[0], new Property[0]);

            TradeManager.proposeTrade(offer, gameState);

            server.sendToPlayer(targetId, "{\"type\":\"INFO\",\"message\":\"Trade Offer: Player " + player.getPlayerId() + " wants to give you $" + amount + "\"}");
            return "{\"type\":\"INFO\",\"message\":\"Trade sent.\"}";

        } catch (Exception e) {
            return "{\"type\":\"ERROR\",\"message\":\"Invalid trade format. Use: ID Amount\"}";
        }
    }

    // --- ۶. پایان نوبت ---
    private String handleEndTurn() {
        gameState.getTurnManager().passTurn();
        Player nextPlayer = gameState.getTurnManager().getCurrentPlayer();
        server.broadcast("{\"type\":\"TURN_UPDATE\",\"currentPlayer\":" + nextPlayer.getPlayerId() + "}");
        return null;
    }

    // --- متدهای کمکی ---

    private void sendToJail(Player player) {
        player.setStatus(PlayerStatus.IN_JAIL);
        player.setCurrentPosition(10);
        player.resetJailTurns();
        server.broadcast("{\"type\":\"ROLL_UPDATE\",\"playerId\":" + player.getPlayerId() +
                ",\"currentPosition\":10,\"message\":\"GO TO JAIL!\"}");
    }

    // این متد حیاتی است برای اینکه پول و املاک در کلاینت آپدیت شود
    private void sendStatsUpdate(Player player) {
        String msg = "{\"type\":\"PLAYER_STATS\",\"playerId\":" + player.getPlayerId() +
                ",\"balance\":" + player.getBalance() + "}";
        server.broadcast(msg);
    }
}
