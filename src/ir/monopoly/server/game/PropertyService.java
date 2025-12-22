package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;

public class PropertyService {

    public static boolean buyProperty(Player player, Property property, GameState gameState) {
        if (property.getOwnerId() != null) return false;
        if (player.getBalance() >= property.getPurchasePrice()) {
            player.changeBalance(-property.getPurchasePrice());
            player.addProperty(property);
            property.setOwner(player.getPlayerId());
            gameState.addEvent(player.getName() + " bought " + property.getName() + " for $" + property.getPurchasePrice());
            return true;
        }
        return false;
    }

    public static String buildOnProperty(Player player, Property property, GameState gameState) {

        if (property.getOwnerId() == null || property.getOwnerId() != player.getPlayerId())
            return "You don't own this property!";

        int ownedInGroup = player.getOwnedProperties().countColorGroup(property.getColorGroup());
        int required = getGroupSize(property.getColorGroup());
        if (ownedInGroup < required)
            return "You need the full color set to build!";

        if (property.hasHotel()) return "Already has a hotel!";
        if (player.getBalance() < property.getHouseCost()) return "Insufficient funds!";

        player.changeBalance(-property.getHouseCost());
        if (property.getHouseCount() < 4) {
            property.addHouse();
            gameState.addEvent(player.getName() + " built a house on " + property.getName());
        } else {
            property.addHotel();
            gameState.addEvent(player.getName() + " built a HOTEL on " + property.getName());
        }
        return "SUCCESS";
    }

    private static int getGroupSize(String colorGroup) {
        if (colorGroup.equalsIgnoreCase("Brown") || colorGroup.equalsIgnoreCase("Dark Blue")) return 2;
        return 3;
    }
}