package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;

public class MortgageManager {

    public static String mortgageProperty(Player player, Property property, GameState gameState) {
        if (property.getOwnerId() == null || property.getOwnerId() != player.getPlayerId()) {
            return "ERROR: You do not own this property.";
        }
        if (property.isMortgaged()) {
            return "ERROR: Property is already mortgaged.";
        }
        if (property.getHouseCount() > 0 || property.hasHotel()) {
            return "ERROR: Cannot mortgage property with houses/hotel. Sell them first.";
        }

        property.setMortgaged(true);
        player.changeBalance(property.getMortgageValue());
        gameState.addEvent(player.getName() + " mortgaged " + property.getName() + " for $" + property.getMortgageValue());
        return "SUCCESS";
    }

    public static String unmortgageProperty(Player player, Property property, GameState gameState) {
        if (property.getOwnerId() == null || property.getOwnerId() != player.getPlayerId()) {
            return "ERROR: You do not own this property.";
        }
        if (!property.isMortgaged()) {
            return "ERROR: Property is not mortgaged.";
        }

        int unmortgageCost = (int) (property.getMortgageValue() * 1.1);
        if (player.getBalance() < unmortgageCost) {
            return "ERROR: Insufficient funds. Need $" + unmortgageCost;
        }

        player.changeBalance(-unmortgageCost);
        property.setMortgaged(false);
        gameState.addEvent(player.getName() + " unmortgaged " + property.getName() + " for $" + unmortgageCost);
        return "SUCCESS";
    }
}