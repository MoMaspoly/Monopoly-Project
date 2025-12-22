package ir.monopoly.server.game;

import ir.monopoly.server.board.Tile;
import ir.monopoly.server.board.TileType;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;
import ir.monopoly.server.property.Property;

public class TileResolver {

    public static void resolveTile(Tile tile, GameState gameState) {
        Player player = gameState.getTurnManager().getCurrentPlayer();

        if (tile.getTileType() == TileType.GO) {
            player.changeBalance(200);
            gameState.addEvent(player.getName() + " passed GO and received $200.");
            return;
        }

        if (tile.getTileType() == TileType.PROPERTY) {
            handleRent(tile, gameState);
            return;
        }

        if (tile.getTileType() == TileType.CARD) {
            Card card = gameState.getCardDeck().drawChance();
            if (card != null) {
                card.execute(player, gameState);
            }
            return;
        }

        if (tile.getTileType() == TileType.TAX) {
            player.changeBalance(-200);
            gameState.addEvent(player.getName() + " paid tax.");
            return;
        }

        if (tile.getTileType() == TileType.JAIL) {
            player.setStatus(PlayerStatus.IN_JAIL);
            player.setCurrentPosition(10);
            player.resetJailTurns();
            gameState.addEvent(player.getName() + " went to jail.");
        }
    }

    public static void handleRent(Tile tile, GameState gameState) {
        if (!(tile.getTileData() instanceof Property)) return;

        Property property = (Property) tile.getTileData();
        Player player = gameState.getTurnManager().getCurrentPlayer();
        Integer ownerId = property.getOwnerId();

        if (ownerId == null || ownerId == player.getPlayerId() || property.isMortgaged()) return;

        Player owner = gameState.getPlayerById(ownerId);

        int ownedInGroup = owner.getOwnedProperties().countColorGroup(property.getColorGroup());
        int groupSize = 3;
        if (property.getColorGroup().equalsIgnoreCase("Brown") || property.getColorGroup().equalsIgnoreCase("Dark Blue")) groupSize = 2;
        boolean hasFullGroup = ownedInGroup >= groupSize;

        int rent = property.calculateRent(hasFullGroup);

        if (player.getBalance() < rent) {
            int available = player.getBalance();
            player.changeBalance(-available);
            owner.changeBalance(available);
            gameState.getTransactionGraph().recordTransaction(player.getPlayerId(), ownerId, available);
            BankruptcyManager.processBankruptcy(player, owner, gameState);
        } else {
            player.changeBalance(-rent);
            owner.changeBalance(rent);
            gameState.getTransactionGraph().recordTransaction(player.getPlayerId(), ownerId, rent);
            gameState.addEvent(player.getName() + " paid $" + rent + " rent to " + owner.getName() + ".");
        }

        gameState.updatePlayerRankings(player);
        gameState.updatePlayerRankings(owner);
    }
}
