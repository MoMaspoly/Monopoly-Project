package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;

/**
 * Manages the negotiation and execution of trades between players.
 * Handles cash transfers and property ownership swaps.
 */
public class TradeManager {

    /**
     * Proposes a trade. Validates that the sender has enough cash and properties aren't mortgaged.
     */
    public static void proposeTrade(TradeOffer offer, GameState gameState) {
        Player sender = gameState.getPlayerById(offer.getFromPlayerId());

        // Validation: Does the sender actually have the cash they are offering?
        if (sender.getBalance() < offer.getOfferedCash()) {
            gameState.addEvent("TRADE_ERROR: Sender has insufficient funds.");
            return;
        }

        // Validation: Properties must not be mortgaged during a trade (Monopoly Standard)
        for (Property p : offer.getOfferedProperties()) {
            if (p.isMortgaged()) {
                gameState.addEvent("TRADE_ERROR: Cannot trade mortgaged properties.");
                return;
            }
        }

        gameState.setPendingTrade(offer);
        gameState.addEvent("TRADE_PENDING: Trade proposed to Player " + offer.getToPlayerId());
    }

    /**
     * Executes the pending trade after the receiver accepts.
     * Swaps money, moves properties between trees, and updates GUI states.
     */
    public static void acceptTrade(GameState gameState) {
        TradeOffer offer = gameState.getPendingTrade();
        if (offer == null) return;

        Player sender = gameState.getPlayerById(offer.getFromPlayerId());
        Player receiver = gameState.getPlayerById(offer.getToPlayerId());

        // 1. Process Cash Exchange
        sender.changeBalance(-offer.getOfferedCash());
        receiver.changeBalance(offer.getOfferedCash());

        if (offer.getRequestedCash() > 0) {
            receiver.changeBalance(-offer.getRequestedCash());
            sender.changeBalance(offer.getRequestedCash());
        }

        // 2. Record in Transaction Graph (for Top-K report)
        if (offer.getOfferedCash() > 0) {
            gameState.getTransactionGraph().recordTransaction(sender.getPlayerId(), receiver.getPlayerId(), offer.getOfferedCash());
        }
        if (offer.getRequestedCash() > 0) {
            gameState.getTransactionGraph().recordTransaction(receiver.getPlayerId(), sender.getPlayerId(), offer.getRequestedCash());
        }

        // 3. Swap Properties: Sender -> Receiver
        for (Property p : offer.getOfferedProperties()) {
            if (p != null) {
                sender.removeProperty(p.getPropertyId());
                receiver.addProperty(p);
                p.setOwner(receiver.getPlayerId());
            }
        }

        // 4. Swap Properties: Receiver -> Sender
        for (Property p : offer.getRequestedProperties()) {
            if (p != null) {
                receiver.removeProperty(p.getPropertyId());
                sender.addProperty(p);
                p.setOwner(sender.getPlayerId());
            }
        }

        // 5. Finalize
        gameState.addEvent("TRADE_SUCCESS: Trade between " + sender.getName() + " and " + receiver.getName() + " completed.");
        gameState.clearPendingTrade();

        // Record for Undo system
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

    /**
     * Rejects the pending trade proposal.
     */
    public static void rejectTrade(GameState gameState) {
        if (gameState.getPendingTrade() != null) {
            gameState.addEvent("TRADE_REJECTED: The offer was declined.");
            gameState.clearPendingTrade();
        }
    }
}
