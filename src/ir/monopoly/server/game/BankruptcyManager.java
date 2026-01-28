package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;

public class BankruptcyManager {

    public static void processBankruptcy(Player bankruptPlayer, Player creditor, GameState gameState) {
        bankruptPlayer.setStatus(PlayerStatus.BANKRUPT);
        gameState.addEvent("Player " + bankruptPlayer.getName() + " went bankrupt.");

        if (creditor != null) {
            int remainingCash = bankruptPlayer.getBalance();
            if (remainingCash > 0) {
                creditor.changeBalance(remainingCash);
                gameState.getTransactionGraph().recordTransaction(bankruptPlayer.getPlayerId(), creditor.getPlayerId(), remainingCash);
            }
        }
        bankruptPlayer.changeBalance(-bankruptPlayer.getBalance());

        bankruptPlayer.getOwnedProperties().forEach(property -> {
            property.clearOwner();
            property.setMortgaged(false);
            property.removeHouses(property.getHouseCount());
            if (property.hasHotel()) property.removeHotel();
            gameState.addEvent("Property " + property.getName() + " returned to the bank.");
            bankruptPlayer.removeProperty(property.getPropertyId());
        });

        gameState.checkGameOver();
    }
}