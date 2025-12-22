package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;

public class ConstructionManager {

    public static String buildHouse(Player player, Property property, GameState gameState) {
        if (property.getOwnerId() == null || property.getOwnerId() != player.getPlayerId()) {
            return "ERROR: You do not own this property.";
        }

        int ownedInGroup = player.getOwnedProperties().countColorGroup(property.getColorGroup());
        int requiredSize = getGroupSize(property.getColorGroup());

        if (ownedInGroup < requiredSize) {
            return "ERROR: You must own the full color set to build.";
        }

        if (property.isMortgaged()) {
            return "ERROR: Cannot build on a mortgaged property.";
        }

        if (!isBuildingBalanced(player, property)) {
            return "ERROR: You must build evenly. Check other properties in this group.";
        }

        if (property.hasHotel()) {
            return "ERROR: Property already reached maximum development (Hotel).";
        }

        if (player.getBalance() < property.getHouseCost()) {
            return "ERROR: Insufficient funds.";
        }

        player.changeBalance(-property.getHouseCost());
        if (property.getHouseCount() < 4) {
            property.addHouse();
            gameState.addEvent(player.getName() + " built a house on " + property.getName());
        } else {
            property.addHotel();
            gameState.addEvent(player.getName() + " upgraded to a HOTEL on " + property.getName());
        }

        return "SUCCESS";
    }

    private static boolean isBuildingBalanced(Player player, Property target) {
        final int targetCountAfterBuild = target.getHouseCount() + 1;
        final boolean[] balanced = {true};

        player.getOwnedProperties().forEach(p -> {
            if (p.getColorGroup().equals(target.getColorGroup())) {
                if (targetCountAfterBuild - p.getHouseCount() > 1) {
                    balanced[0] = false;
                }
            }
        });
        return balanced[0];
    }

    private static int getGroupSize(String colorGroup) {
        if (colorGroup.equalsIgnoreCase("Brown") || colorGroup.equalsIgnoreCase("Dark Blue")) return 2;
        return 3;
    }
}
