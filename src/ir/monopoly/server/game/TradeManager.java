package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;


public class TradeManager {

    public static void proposeTrade(TradeOffer offer, GameState gameState) {
        Player sender = gameState.getPlayerById(offer.getFromPlayerId());

        if (sender.getBalance() < offer.getOfferedCash()) {
            gameState.addEvent("TRADE_ERROR: Sender has insufficient funds.");
            return;
        }

        for (Property p : offer.getOfferedProperties()) {
            if (p.isMortgaged()) {
                gameState.addEvent("TRADE_ERROR: Cannot trade mortgaged properties.");
                return;
            }
        }

        gameState.setPendingTrade(offer);
        gameState.addEvent("TRADE_PENDING: Trade proposed to Player " + offer.getToPlayerId());
    }

    public static void acceptTrade(GameState gameState) {
        TradeOffer offer = gameState.getPendingTrade();
        if (offer == null) return;

        Player sender = gameState.getPlayerById(offer.getFromPlayerId());
        Player receiver = gameState.getPlayerById(offer.getToPlayerId());

        sender.changeBalance(-offer.getOfferedCash());
        receiver.changeBalance(offer.getOfferedCash());

        if (offer.getRequestedCash() > 0) {
            receiver.changeBalance(-offer.getRequestedCash());
            sender.changeBalance(offer.getRequestedCash());
        }

        if (offer.getOfferedCash() > 0) {
            gameState.getTransactionGraph().recordTransaction(sender.getPlayerId(), receiver.getPlayerId(), offer.getOfferedCash());
        }
        if (offer.getRequestedCash() > 0) {
            gameState.getTransactionGraph().recordTransaction(receiver.getPlayerId(), sender.getPlayerId(), offer.getRequestedCash());
        }

        for (Property p : offer.getOfferedProperties()) {
            if (p != null) {
                sender.removeProperty(p.getPropertyId());
                receiver.addProperty(p);
                p.setOwner(receiver.getPlayerId());
            }
        }

        for (Property p : offer.getRequestedProperties()) {
            if (p != null) {
                receiver.removeProperty(p.getPropertyId());
                sender.addProperty(p);
                p.setOwner(sender.getPlayerId());
            }
        }

        gameState.addEvent("TRADE_SUCCESS: Trade between " + sender.getName() + " and " + receiver.getName() + " completed.");
        gameState.clearPendingTrade();

        gameState.getUndoManager().recordAction(new GameAction(
                GameAction.ActionType.TRADE,
                sender.getPlayerId(),
                receiver.getPlayerId(),
                offer.getOfferedProperties(),
                offer.getRequestedProperties(),
                offer.getOfferedCash(),
                offer.getRequestedCash()
        ));
    }

    public static void rejectTrade(GameState gameState) {
        if (gameState.getPendingTrade() != null) {
            gameState.addEvent("TRADE_REJECTED: The offer was declined.");
            gameState.clearPendingTrade();
        }
    }
}
