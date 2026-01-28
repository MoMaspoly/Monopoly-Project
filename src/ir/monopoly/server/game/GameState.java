package ir.monopoly.server.game;

import ir.monopoly.server.board.Board;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;
import ir.monopoly.server.datastructure.TransactionGraph;
import java.util.ArrayList;
import java.util.List;

/**
 * Main state container for the game.
 * Holds players, board, card deck, and tracks game events for the GUI.
 */
public class GameState {
    private final Player[] players;
    private final Board board;
    private final CardDeck cardDeck;
    private final TurnManager turnManager;
    private final UndoManager undoManager;
    private final TransactionGraph transactionGraph;
    private final List<String> eventLog;
    private TradeOffer pendingTrade;
    private AuctionManager auctionManager;

    public GameState(Player[] players, Board board) {
        this.players = players;
        this.board = board;
        this.cardDeck = new CardDeck();
        this.turnManager = new TurnManager(players);
        this.undoManager = new UndoManager(this);
        this.transactionGraph = new TransactionGraph(players.length);
        this.eventLog = new ArrayList<>();
        this.addEvent("Game Started");
    }

    /**
     * Starts an auction for a property
     */
    public void startAuction(Property property) {
        this.auctionManager = new AuctionManager(property, players);
        addEvent("AUCTION_STARTED: Auction for " + property.getName() + " started. Minimum bid: $10");
    }

    /**
     * Ends current auction
     */
    public void endAuction() {
        if (auctionManager != null) {
            auctionManager.forceEndAuction();
            Property property = auctionManager.getProperty();
            Player winner = auctionManager.getCurrentWinner();
            if (winner != null) {
                addEvent("AUCTION_ENDED: " + winner.getName() + " won " + property.getName() + " for $" + auctionManager.getCurrentHighestBid());
            }
            auctionManager = null;
        }
    }

    public boolean isAuctionActive() {
        return auctionManager != null && auctionManager.isAuctionActive();
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    /**
     * Adds a new event to the log.
     * Used by TileResolver to trigger GUI updates.
     */
    public void addEvent(String event) {
        eventLog.add(event);
        System.out.println("LOG: " + event);
    }

    /**
     * Retrieves the most recent event.
     */
    public String getLastEvent() {
        if (eventLog.isEmpty()) return "";
        return eventLog.get(eventLog.size() - 1);
    }

    public Player getPlayerById(int id) {
        for (Player p : players) {
            if (p.getPlayerId() == id) return p;
        }
        return null;
    }

    public Property getPropertyById(int id) {
        var tile = board.getTileAt(id);
        if (tile != null && tile.getTileData() instanceof Property) {
            return (Property) tile.getTileData();
        }
        return null;
    }

    // --- Getters ---
    public Player[] getPlayers() { return players; }
    public Board getBoard() { return board; }
    public CardDeck getCardDeck() { return cardDeck; }
    public TurnManager getTurnManager() { return turnManager; }
    public UndoManager getUndoManager() { return undoManager; }
    public TransactionGraph getTransactionGraph() { return transactionGraph; }

    // --- Trade Management ---
    public TradeOffer getPendingTrade() { return pendingTrade; }
    public void setPendingTrade(TradeOffer offer) { this.pendingTrade = offer; }
    public void clearPendingTrade() { this.pendingTrade = null; }

    public void checkGameOver() {
        int activeCount = 0;
        for (Player p : players) {
            if (p.getStatus() != ir.monopoly.server.player.PlayerStatus.BANKRUPT) activeCount++;
        }
        if (activeCount <= 1) addEvent("GAME_OVER");
    }

    /**
     * Requirement fix: Top-K ranking update trigger
     */
    public void updatePlayerRankings(Player p) {
        // This is handled by the TransactionGraph and LeaderboardManager
    }
}