package ir.monopoly.server.game;

import ir.monopoly.server.board.Tile;
import ir.monopoly.server.board.TileType;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;
import ir.monopoly.server.property.Property;

public class TileResolver {

    public static void resolveTile(Tile tile, GameState gameState) {
        Player player = gameState.getTurnManager().getCurrentPlayer();

        // 1. CARDS
        if (tile.getTileType() == TileType.CARD) {
            Card card = tile.getName().contains("Chance") ?
                    gameState.getCardDeck().drawChance() :
                    gameState.getCardDeck().drawCommunityChest();
            card.execute(player, gameState);
            gameState.addEvent("ACTION_CARD:" + card.getDescription());
            return;
        }

        // 2. GO TO JAIL
        if (tile.getTileType() == TileType.JAIL && tile.getTileId() == 30) {
            player.setStatus(PlayerStatus.IN_JAIL);
            player.setCurrentPosition(10);
            player.resetJailTurns();
            gameState.addEvent("ACTION_JAIL:" + player.getName() + " was sent to Jail!");
            return;
        }

        // 3. TAXES
        if (tile.getTileType() == TileType.TAX) {
            int tax = (tile.getTileId() == 4) ? 200 : 100;
            player.changeBalance(-tax);
            gameState.addEvent("ACTION_TAX:Paid $" + tax + " Tax.");
            return;
        }

        // 4. PROPERTIES
        if (tile.getTileType() == TileType.PROPERTY) {
            handleProperty(tile, gameState, player);
        }
    }

    private static void handleProperty(Tile tile, GameState gs, Player visitor) {
        Property prop = (Property) tile.getTileData();
        Integer ownerId = prop.getOwnerId();

        if (ownerId == null) {
            gs.addEvent("ACTION_OFFER:Unowned " + prop.getName() + " for $" + prop.getPurchasePrice());
        } else if (ownerId != visitor.getPlayerId() && !prop.isMortgaged()) {
            Player owner = gs.getPlayerById(ownerId);
            int rent = 0;

            if (prop.getColorGroup().equalsIgnoreCase("Railroad")) {
                int count = owner.getOwnedProperties().countColorGroup("Railroad");
                rent = (int) (25 * Math.pow(2, count - 1));
            } else if (prop.getColorGroup().equalsIgnoreCase("Utility")) {
                int diceSum = new Dice().roll()[0] + new Dice().roll()[1]; // Dummy dice for rent calc
                int count = owner.getOwnedProperties().countColorGroup("Utility");
                rent = (count == 1) ? diceSum * 4 : diceSum * 10;
            } else {
                int ownedInSet = owner.getOwnedProperties().countColorGroup(prop.getColorGroup());
                int required = (prop.getColorGroup().equals("Brown") || prop.getColorGroup().equals("Dark Blue")) ? 2 : 3;
                rent = prop.calculateRent(ownedInSet >= required);
            }

            visitor.changeBalance(-rent);
            owner.changeBalance(rent);
            gs.addEvent("ACTION_RENT:Paid $" + rent + " rent to " + owner.getName());
        }
    }
}
