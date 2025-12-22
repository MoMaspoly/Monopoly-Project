package ir.monopoly.server.game;

import ir.monopoly.server.board.Tile;
import ir.monopoly.server.board.TileType;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;
import ir.monopoly.server.property.Property;

public class GameController {
    private final GameState gameState;
    private AuctionManager currentAuction = null;

    public GameController(GameState gameState) {
        this.gameState = gameState;
    }

    public String handleCommand(String command, int playerId, String extraData) {
        Player player = gameState.getPlayerById(playerId);
        if (player == null) return "ERROR: Player not found.";

        if (gameState.getTurnManager().getCurrentPlayer().getPlayerId() != playerId) {
            return "ERROR: It is not your turn.";
        }

        if (gameState.isGameOver()) {
            return "ERROR: Game is over.";
        }

        try {
            switch (command.toUpperCase()) {
                case "ROLL":
                    if (player.getStatus() == PlayerStatus.BANKRUPT) return "ERROR: You are bankrupt.";
                    if (player.getStatus() == PlayerStatus.IN_JAIL) return "ERROR: You are in jail.";
                    return handleRoll(player);

                case "BUY":
                    return handleBuy(player);

                case "NO_BUY":
                    return startAuctionForCurrentTile();

                case "BID":
                    int bidAmount = Integer.parseInt(extraData);
                    return handleBid(player, bidAmount);

                case "PROPOSE_TRADE":
                    return handleProposeTrade(player, extraData);

                case "ACCEPT_TRADE":
                    return handleAcceptTrade(player);

                case "REJECT_TRADE":
                    return handleRejectTrade(player);

                case "BUILD":
                    int propId = Integer.parseInt(extraData);
                    Property p = gameState.getPropertyById(propId);
                    if (p == null) return "ERROR: Property not found.";
                    String buildResult = ConstructionManager.buildHouse(player, p, gameState);
                    if (buildResult.equals("SUCCESS")) {
                        gameState.updatePlayerRankings(player);
                    }
                    return buildResult;

                case "MORTGAGE":
                    int mPropId = Integer.parseInt(extraData);
                    Property mp = gameState.getPropertyById(mPropId);
                    if (mp == null) return "ERROR: Property not found.";
                    MortgageManager.mortgageProperty(player, mp, gameState);
                    gameState.updatePlayerRankings(player);
                    return "SUCCESS: Property mortgaged.";

                case "UNMORTGAGE":
                    int umPropId = Integer.parseInt(extraData);
                    Property ump = gameState.getPropertyById(umPropId);
                    if (ump == null) return "ERROR: Property not found.";
                    MortgageManager.unmortgageProperty(player, ump, gameState);
                    gameState.updatePlayerRankings(player);
                    return "SUCCESS: Mortgage lifted.";

                case "UNDO":
                    gameState.getUndoManager().undo();
                    gameState.updatePlayerRankings(player);
                    return "SUCCESS: Undo performed.";

                case "REDO":
                    gameState.getUndoManager().redo();
                    gameState.updatePlayerRankings(player);
                    return "SUCCESS: Redo performed.";

                case "REPORT_FINANCIAL":
                    gameState.getTransactionGraph().printFinancialSummary(getPlayerNames());
                    return "SUCCESS: Financial report printed.";

                case "REPORT_WEALTH":
                    System.out.println(gameState.getWealthReport());
                    return "SUCCESS: Wealth list printed.";

                case "LEADERBOARD":
                    LeaderboardManager.printTopPlayers(gameState);
                    System.out.println("Top Interaction: " + gameState.getTransactionGraph().getTopInteraction(getPlayerNames()));
                    return "SUCCESS: Leaderboard and graph analysis printed.";

                case "END_TURN":
                    gameState.getTurnManager().passTurn();
                    gameState.checkGameOver();
                    return "SUCCESS: Turn changed.";

                case "JAIL_PAY":
                    if (player.getBalance() < 50) return "ERROR: You don't have enough money to pay the fine.";
                    player.changeBalance(-50);
                    player.releaseFromJail();
                    gameState.addEvent(player.getName() + " paid the jail fine and was released.");
                    return "SUCCESS: You were released from jail.";

                case "JAIL_TRY":
                    Dice dice = new Dice();
                    int die1 = dice.roll();
                    int die2 = dice.roll();
                    int total = die1 + die2;
                    if (die1 == die2) {
                        player.releaseFromJail();
                        MoveService.movePlayer(player, gameState.getBoard(), total);
                        gameState.addEvent(player.getName() + " was released from jail with a double.");
                    } else {
                        player.incrementJailTurns();
                        gameState.addEvent(player.getName() + " didn't roll a double. Turn " + player.getJailTurns() + " in jail.");
                        if (player.getJailTurns() >= 3) {
                            return handleCommand("JAIL_PAY", playerId, "");
                        }
                    }
                    return "SUCCESS: Attempt to exit jail performed.";

                default:
                    return "ERROR: Unknown command.";
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private String handleRoll(Player player) {
        Dice dice = new Dice();
        int total = dice.roll();
        int oldPos = player.getCurrentPosition();

        Tile destination = MoveService.movePlayer(player, gameState.getBoard(), total);

        gameState.getUndoManager().recordAction(new GameAction(
                GameAction.ActionType.MOVEMENT,
                player.getPlayerId(),
                oldPos,
                destination.getTileId(),
                0
        ));

        TileResolver.resolveTile(destination, gameState);

        return "SUCCESS: Dice rolled " + total + ". New position: " + destination.getTileId();
    }

    private String handleBuy(Player player) {
        Tile currentTile = gameState.getBoard().getTileAt(player.getCurrentPosition());
        if (currentTile.getTileType() != TileType.PROPERTY) return "ERROR: This tile is not a property.";

        Property prop = (Property) currentTile.getTileData();
        if (prop.getOwnerId() != null) return "ERROR: This property has already been sold.";

        if (PropertyService.buyProperty(player, prop, gameState)) {
            gameState.getUndoManager().recordAction(new GameAction(
                    GameAction.ActionType.PROPERTY_PURCHASE,
                    player.getPlayerId(),
                    null,
                    null,
                    prop.getPropertyId()
            ));
            gameState.updatePlayerRankings(player);
            return "SUCCESS: Property purchased.";
        }
        return "ERROR: Purchase unsuccessful.";
    }

    private String startAuctionForCurrentTile() {
        Player currentPlayer = gameState.getTurnManager().getCurrentPlayer();
        Tile tile = gameState.getBoard().getTileAt(currentPlayer.getCurrentPosition());
        if (tile.getTileType() != TileType.PROPERTY) return "ERROR: This tile cannot be auctioned.";

        Property prop = (Property) tile.getTileData();
        currentAuction = new AuctionManager(prop, gameState.getPlayers());
        gameState.addEvent("Auction for property " + prop.getName() + " started.");
        return "AUCTION_STARTED";
    }

    private String handleBid(Player player, int amount) {
        if (currentAuction == null) return "ERROR: No auction in progress.";
        if (currentAuction.placeBid(player, amount)) {
            gameState.addEvent(player.getName() + " bid " + amount + ".");
            return "SUCCESS: Bid registered.";
        }
        return "ERROR: Invalid bid.";
    }

    private String handleProposeTrade(Player player, String extraData) {
        gameState.addEvent("Trade proposed.");
        return "SUCCESS: Trade offer sent.";
    }

    private String handleAcceptTrade(Player player) {
        TradeManager.acceptTrade(gameState);
        return "SUCCESS: Trade completed.";
    }

    private String handleRejectTrade(Player player) {
        TradeManager.rejectTrade(gameState);
        return "SUCCESS: Trade rejected.";
    }

    private String[] getPlayerNames() {
        Player[] players = gameState.getPlayers();
        String[] names = new String[players.length];
        for (int i = 0; i < players.length; i++) names[i] = players[i].getName();
        return names;
    }
}