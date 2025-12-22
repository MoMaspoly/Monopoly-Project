package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;

public class TradeManager {

    public static void proposeTrade(TradeOffer offer, GameState gameState) {
        Player sender = gameState.getPlayerById(offer.getFromPlayerId());
        if (sender.getBalance() < offer.getOfferedCash()) {
            gameState.addEvent("Invalid trade: insufficient funds.");
            return;
        }
        for (Property p : offer.getOfferedProperties()) {
            if (p.isMortgaged()) {
                gameState.addEvent("Invalid trade: mortgaged property cannot be traded.");
                return;
            }
        }
        gameState.setPendingTrade(offer);
        gameState.addEvent("Trade proposed from " + sender.getName() + " to player " + offer.getToPlayerId());
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

        if (offer.getOfferedCash() > 0)
            gameState.getTransactionGraph().recordTransaction(sender.getPlayerId(), receiver.getPlayerId(), offer.getOfferedCash());
        if (offer.getRequestedCash() > 0)
            gameState.getTransactionGraph().recordTransaction(receiver.getPlayerId(), sender.getPlayerId(), offer.getRequestedCash());

        for (Property p : offer.getOfferedProperties()) {
            sender.removeProperty(p.getPropertyId());
            receiver.addProperty(p);
            p.setOwner(receiver.getPlayerId());
        }
        for (Property p : offer.getRequestedProperties()) {
            receiver.removeProperty(p.getPropertyId());
            sender.addProperty(p);
            p.setOwner(sender.getPlayerId());
        }

        gameState.addEvent("Trade accepted and executed.");
        gameState.clearPendingTrade();
    }

    public static void rejectTrade(GameState gameState) {
        if (gameState.getPendingTrade() != null) {
            gameState.addEvent("Trade rejected.");
            gameState.clearPendingTrade();
        }
    }
}