package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;

public class TurnManager {
    private final Player[] players;
    private int currentPlayerIndex;
    private GamePhase currentPhase;
    private int turnCounter;
    private int consecutiveDoubles = 0;
    private boolean lastRollWasDoubles = false;

    public TurnManager(Player[] players) {
        this.players = players;
        this.currentPlayerIndex = 0;
        this.currentPhase = GamePhase.TURN_START;
        this.turnCounter = 1;
        this.consecutiveDoubles = 0;
    }

    public void setPhase(GamePhase phase) {
        this.currentPhase = phase;
    }

    public void nextPhase() {
        switch (currentPhase) {
            case TURN_START -> currentPhase = GamePhase.PRE_ROLL;
            case PRE_ROLL -> currentPhase = GamePhase.ROLL;
            case ROLL -> currentPhase = GamePhase.MOVE_AND_RESOLVE;
            case MOVE_AND_RESOLVE -> currentPhase = GamePhase.DECISION;
            case DECISION -> currentPhase = GamePhase.TURN_END;
            case TURN_END -> {
                if (!lastRollWasDoubles) {
                    advanceToNextPlayer();
                    consecutiveDoubles = 0;
                } else {
                    consecutiveDoubles++;
                    lastRollWasDoubles = false;
                }
                currentPhase = GamePhase.TURN_START;
            }
        }
    }

    private void advanceToNextPlayer() {
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.length;
        } while (players[currentPlayerIndex].getStatus() == PlayerStatus.BANKRUPT);
        turnCounter++;
    }

    public void passTurn() {
        advanceToNextPlayer();
        consecutiveDoubles = 0;
        lastRollWasDoubles = false;
        currentPhase = GamePhase.TURN_START;
    }


    public void registerDoubles() {
        lastRollWasDoubles = true;
    }

    public boolean hasThreeConsecutiveDoubles() {
        return consecutiveDoubles >= 2;
    }

    public void resetDoubles() {
        consecutiveDoubles = 0;
        lastRollWasDoubles = false;
    }

    public Player getCurrentPlayer() { return players[currentPlayerIndex]; }
    public GamePhase getCurrentPhase() { return currentPhase; }
    public int getTurnCounter() { return turnCounter; }
}