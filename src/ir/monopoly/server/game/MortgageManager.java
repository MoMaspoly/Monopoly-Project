package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;

public class MortgageManager {

    public static void mortgageProperty(Player player, Property property, GameState gameState) {
        if (property.getOwnerId() != player.getPlayerId() || property.isMortgaged()) return;

        if (property.getHouseCount() > 0) return;

        property.setMortgaged(true);
        player.changeBalance(property.getMortgageValue());
        gameState.addEvent(player.getName() + " mortgaged " + property.getName());

    }

    public static void unmortgageProperty(Player player, Property property, GameState gameState) {
        int cost = property.getUnmortgagePrice();
        if (property.isMortgaged() && player.getBalance() >= cost) {
            player.changeBalance(-cost);
            property.setMortgaged(false);
            gameState.addEvent(player.getName() + " unmortgaged " + property.getName());
        }
    }
}