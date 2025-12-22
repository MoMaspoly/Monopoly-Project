package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;

public class Card {
    private final String description;
    private final CardAction action;

    public Card(String description, CardAction action) {
        this.description = description;
        this.action = action;
    }

    public String getDescription() { return description; }

    public void execute(Player player, GameState gameState) {
        action.execute(player, gameState);
    }

    @FunctionalInterface
    public interface CardAction {
        void execute(Player player, GameState gameState);
    }
}