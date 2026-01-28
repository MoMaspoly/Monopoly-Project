package ir.monopoly.server.game;

import ir.monopoly.server.board.Board;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;
import ir.monopoly.server.datastructure.TransactionGraph;
import java.util.ArrayList;
import java.util.List;

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

    public void startAuction(Property property) {
        this.auctionManager = new AuctionManager(property, players, this);
        addEvent("AUCTION_STARTED: Auction for " + property.getName() + " started. Minimum bid: $10");
    }

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

    public void addEvent(String event) {
        eventLog.add(event);
        System.out.println("LOG: " + event);
    }

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

    public Player[] getPlayers() { return players; }
    public Board getBoard() { return board; }
    public CardDeck getCardDeck() { return cardDeck; }
    public TurnManager getTurnManager() { return turnManager; }
    public UndoManager getUndoManager() { return undoManager; }
    public TransactionGraph getTransactionGraph() { return transactionGraph; }

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

    public void updatePlayerRankings(Player p) {
    }
}