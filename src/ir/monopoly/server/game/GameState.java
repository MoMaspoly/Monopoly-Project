package ir.monopoly.server.game;

import ir.monopoly.server.board.Board;
import ir.monopoly.server.board.Tile;
import ir.monopoly.server.datastructure.*;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;
import ir.monopoly.server.property.Property;

import java.util.Random;

public class GameState {
    private final Player[] players;
    private final Board board;
    private boolean gameOver;
    private final TurnManager turnManager;
    private final CardDeck cardDeck;
    private final MyQueue<String> eventLog;
    private final UndoManager undoManager;
    private final TransactionGraph transactionGraph;
    private TradeOffer pendingTrade = null;

    private final MyHashTable<Integer, Player> playerTable;
    private final MyHashTable<Integer, Property> propertyTable;

    private final WealthBST wealthBST;

    private final MyHeap<Player> wealthyPlayersHeap;

    public GameState(Player[] players, Board board) {
        this.players = players.clone();
        Random rand = new Random();
        for (int i = this.players.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            Player temp = this.players[i];
            this.players[i] = this.players[j];
            this.players[j] = temp;
        }

        this.board = board;
        this.gameOver = false;

        this.playerTable = new MyHashTable<>();
        this.propertyTable = new MyHashTable<>();

        for (Player p : this.players) {
            playerTable.put(p.getPlayerId(), p);
        }

        for (int i = 0; i < board.getSize(); i++) {
            Tile t = board.getTileAt(i);
            if (t.getTileData() instanceof Property) {
                Property prop = (Property) t.getTileData();
                propertyTable.put(prop.getPropertyId(), prop);
            }
        }

        this.wealthBST = new WealthBST();
        for (Player p : this.players) {
            wealthBST.insert(p);
        }

        this.wealthyPlayersHeap = new MyHeap<>(true);

        this.turnManager = new TurnManager(this.players);
        this.cardDeck = new CardDeck();
        this.eventLog = new MyQueue<>();
        this.undoManager = new UndoManager(this);
        this.transactionGraph = new TransactionGraph(this.players.length);

        this.addEvent("Game Started.");
    }

    public Property getPropertyById(int id) {
        return propertyTable.get(id);
    }

    public Player getPlayerById(int id) {
        return playerTable.get(id);
    }

    public void onPlayerBalanceChanged(int playerId) {
        Player p = playerTable.get(playerId);
        if (p != null) {
            wealthBST.update(p);
            addEvent("Player " + p.getName() + "'s wealth status updated.");
        }
    }

    public void updatePlayerRankings(Player player) {
        wealthBST.update(player);
        rebuildWealthHeap();
    }

    private void rebuildWealthHeap() {
        while (!wealthyPlayersHeap.isEmpty()) {
            wealthyPlayersHeap.extract();
        }
        for (Player p : players) {
            if (p.getStatus() == PlayerStatus.ACTIVE) {
                wealthyPlayersHeap.insert(p);
            }
        }
    }

    public Player[] getTopWealthyPlayers(int k) {
        rebuildWealthHeap();
        int count = Math.min(k, wealthyPlayersHeap.size());
        Player[] topPlayers = new Player[count];
        for (int i = 0; i < count; i++) {
            topPlayers[i] = wealthyPlayersHeap.extract();
        }
        return topPlayers;
    }

    public String getWealthReport() {
        return wealthBST.getSortedWealthString();
    }

    public Player[] getPlayers() { return players; }
    public Board getBoard() { return board; }
    public TurnManager getTurnManager() { return turnManager; }
    public CardDeck getCardDeck() { return cardDeck; }
    public UndoManager getUndoManager() { return undoManager; }
    public TransactionGraph getTransactionGraph() { return transactionGraph; }
    public WealthBST getWealthBST() { return wealthBST; }
    public boolean isGameOver() { return gameOver; }
    public void addEvent(String event) { eventLog.enqueue(event); }

    public void setPendingTrade(TradeOffer offer) { this.pendingTrade = offer; }
    public TradeOffer getPendingTrade() { return pendingTrade; }
    public void clearPendingTrade() { pendingTrade = null; }

    public void checkGameOver() {
        int activeCount = 0;
        Player winner = null;
        for (Player p : players) {
            if (p.getStatus() == PlayerStatus.ACTIVE) {
                activeCount++;
                winner = p;
            }
        }
        if (activeCount <= 1) {
            gameOver = true;
            if (winner != null) {
                addEvent("Game Over! Winner: " + winner.getName());
            }
        }
    }
}