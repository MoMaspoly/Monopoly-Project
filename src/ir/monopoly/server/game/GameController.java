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

        if (currentAuction != null && !command.equalsIgnoreCase("BID") && !command.equalsIgnoreCase("PASS")) {
            return "ERROR: Auction in progress. You must BID or PASS.";
        }

        try {
            switch (command.toUpperCase()) {
                case "ROLL":
                    if (player.getStatus() == PlayerStatus.BANKRUPT) return "ERROR: You are bankrupt.";
                    if (player.getStatus() == PlayerStatus.IN_JAIL) return "ERROR: You are in jail. Use JAIL_TRY, JAIL_PAY or JAIL_CARD.";
                    return handleRoll(player);

                case "JAIL_TRY":
                    if (player.getStatus() != PlayerStatus.IN_JAIL) return "ERROR: You are not in jail.";

                    Dice jailDice = new Dice();
                    int[] jRoll = jailDice.roll();
                    int jDie1 = jRoll[0];
                    int jDie2 = jRoll[1];
                    int jSum = jailDice.getSum();

                    gameState.addEvent(player.getName() + " tries to escape jail: rolled " + jDie1 + " + " + jDie2);

                    if (jailDice.isDoubles()) {
                        player.releaseFromJail();
                        gameState.addEvent(player.getName() + " rolled doubles and escaped from jail!");

                        Tile dest = MoveService.movePlayer(player, gameState.getBoard(), jSum);
                        TileResolver.resolveTile(dest, gameState);

                        gameState.getTurnManager().registerDoubles();

                        return "JAIL_ESCAPE_DOUBLES:" + jSum;
                    } else {
                        player.incrementJailTurns();
                        gameState.addEvent(player.getName() + " failed to roll doubles. Jail turn: " + player.getJailTurns() + "/3");

                        if (player.getJailTurns() >= 3) {

                            if (player.getBalance() < 50) {
                                BankruptcyManager.processBankruptcy(player, null, gameState);
                                return "BANKRUPT_JAIL";
                            }
                            player.changeBalance(-50);
                            player.releaseFromJail();
                            gameState.addEvent(player.getName() + " paid $50 after 3 failed attempts and got out.");

                            Tile forcedDest = MoveService.movePlayer(player, gameState.getBoard(), jSum);
                            TileResolver.resolveTile(forcedDest, gameState);
                        }

                        gameState.getTurnManager().passTurn();
                        return "JAIL_FAILED:" + player.getJailTurns();
                    }
                case "PASS":
                    if (currentAuction == null) return "ERROR: No auction in progress.";
                    currentAuction.passBid(player);
                    gameState.addEvent(player.getName() + " passed in auction.");

                    if (currentAuction.isFinished()) {
                        Player winner = currentAuction.getCurrentWinner();
                        Property p = currentAuction.getProperty();

                        if (winner != null) {
                            gameState.getUndoManager().recordAction(new GameAction(
                                    GameAction.ActionType.PROPERTY_PURCHASE,
                                    winner.getPlayerId(),
                                    null, null, p.getPropertyId()
                            ));
                        }
                        currentAuction = null;
                        return "SUCCESS: Auction finished.";
                    }
                    return "SUCCESS: Pass registered.";
                case "JAIL_PAY":
                    if (player.getStatus() != PlayerStatus.IN_JAIL) return "ERROR: You are not in jail.";
                    if (player.getBalance() < 50) return "ERROR: Not enough money to pay $50.";

                    player.changeBalance(-50);
                    player.releaseFromJail();
                    gameState.addEvent(player.getName() + " paid $50 and got out of jail.");
                    return "JAIL_PAID";

                case "JAIL_CARD":
                    if (player.getStatus() != PlayerStatus.IN_JAIL) return "ERROR: You are not in jail.";
                    if (!player.hasGetOutOfJailFreeCard()) return "ERROR: No Get Out of Jail Free card.";

                    player.useGetOutOfJailFreeCard(true);
                    player.releaseFromJail();
                    gameState.addEvent(player.getName() + " used a Get Out of Jail Free card.");
                    return "JAIL_CARD_USED";

                case "BUY":
                    return handleBuy(player);

                case "NO_BUY":
                    return startAuctionForCurrentTile();

                case "BID":
                    if (extraData.isEmpty()) return "ERROR: Bid amount required.";
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
                    System.out.println(gameState.getTransactionGraph().getTopInteraction(getPlayerNames()));
                    return "SUCCESS: Leaderboard and top financial interaction printed.";

                case "END_TURN":
                    gameState.getTurnManager().passTurn();
                    gameState.checkGameOver();
                    return "SUCCESS: Turn changed.";

                default:
                    return "ERROR: Unknown command: " + command;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String handleRoll(Player player) {
        Dice dice = new Dice();
        int[] roll = dice.roll();
        int die1 = roll[0];
        int die2 = roll[1];
        int total = dice.getSum();
        boolean isDoubles = dice.isDoubles();

        int oldPos = player.getCurrentPosition();

        gameState.addEvent(player.getName() + " rolled " + die1 + " + " + die2 + " = " + total);

        if (isDoubles) {
            gameState.getTurnManager().registerDoubles();
            gameState.addEvent("Doubles! " + player.getName() + " gets another turn.");

            if (gameState.getTurnManager().hasThreeConsecutiveDoubles()) {
                gameState.addEvent(player.getName() + " rolled doubles three times in a row → Sent to jail!");
                player.setStatus(PlayerStatus.IN_JAIL);
                player.setCurrentPosition(10);
                gameState.getTurnManager().resetDoubles();
                gameState.getTurnManager().passTurn();
                return "DOUBLES_JAIL:" + total;
            }
        } else {
            gameState.getTurnManager().resetDoubles();
        }

        Tile destination = MoveService.movePlayer(player, gameState.getBoard(), total);

        gameState.getUndoManager().recordAction(new GameAction(
                GameAction.ActionType.MOVEMENT,
                player.getPlayerId(),
                oldPos,
                destination.getTileId(),
                0
        ));

        TileResolver.resolveTile(destination, gameState);

        if (!isDoubles) {
            gameState.getTurnManager().passTurn();
        }

        return "SUCCESS: Dice rolled " + total + " (" + die1 + "+" + die2 + "). New position: " + destination.getTileId() + (isDoubles ? " | DOUBLES - Another turn!" : "");
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
            gameState.addEvent(player.getName() + " bid $" + amount);
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