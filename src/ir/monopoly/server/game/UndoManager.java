package ir.monopoly.server.game;

import ir.monopoly.server.datastructure.MyStack;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;

/**
 * Manages Undo and Redo operations for the game.
 * Records actions such as money changes, property purchases, construction, and movement.
 */
public class UndoManager {
    private final MyStack<GameAction> undoStack;
    private final MyStack<GameAction> redoStack;
    private final GameState gameState;

    public UndoManager(GameState gameState) {
        this.gameState = gameState;
        this.undoStack = new MyStack<>();
        this.redoStack = new MyStack<>();
    }

    /**
     * Records a new action and clears the redo stack.
     * @param action The action performed by a player.
     */
    public void recordAction(GameAction action) {
        undoStack.push(action);
        // Once a new action is taken, redo history is invalidated
        while (!redoStack.isEmpty()) {
            redoStack.pop();
        }
    }

    /**
     * Reverts the last recorded action.
     */
    public void undo() {
        if (undoStack.isEmpty()) {
            gameState.addEvent("UNDO: No action to undo.");
            return;
        }

        GameAction action = undoStack.pop();
        redoStack.push(action);

        switch (action.getType()) {
            case MONEY_CHANGE:
                // Revert balance: diff = NewValue - OldValue. Subtract diff from current.
                int diff = (Integer) action.getNewValue() - (Integer) action.getOldValue();
                Player pMoney = gameState.getPlayerById(action.getPlayerId());
                if (pMoney != null) {
                    pMoney.changeBalance(-diff);
                }
                gameState.addEvent("UNDO: Money change reverted.");
                break;

            case PROPERTY_PURCHASE:
                // Revert purchase: give money back and remove ownership
                Player pPurchase = gameState.getPlayerById(action.getPlayerId());
                Property purchasedProp = gameState.getPropertyById(action.getTargetId());
                if (pPurchase != null && purchasedProp != null) {
                    pPurchase.changeBalance(purchasedProp.getPurchasePrice());
                    pPurchase.removeProperty(purchasedProp.getPropertyId());
                    purchasedProp.clearOwner(); // Critical: Update property object status
                    gameState.addEvent("UNDO: Property purchase reverted for " + purchasedProp.getName());
                }
                break;

            case CONSTRUCTION:
                // Revert house/hotel build: refund cost and remove building
                Player pBuild = gameState.getPlayerById(action.getPlayerId());
                Property builtProp = gameState.getPropertyById(action.getTargetId());
                if (pBuild != null && builtProp != null) {
                    pBuild.changeBalance(builtProp.getHouseCost());
                    if (builtProp.hasHotel()) {
                        builtProp.removeHotel();
                    } else {
                        builtProp.removeHouses(1);
                    }
                    gameState.addEvent("UNDO: Construction on " + builtProp.getName() + " reverted.");
                }
                break;

            case MOVEMENT:
                // Revert player position
                Player pMove = gameState.getPlayerById(action.getPlayerId());
                if (pMove != null) {
                    pMove.setCurrentPosition((Integer) action.getOldValue());
                    gameState.addEvent("UNDO: Movement reverted.");
                }
                break;

            case TRADE:
                // Revert a completed trade between two players
                Player sender = gameState.getPlayerById(action.getPlayerId());
                Player receiver = gameState.getPlayerById(action.getOtherPlayerId());

                if (sender != null && receiver != null) {
                    // Revert cash exchange
                    sender.changeBalance(action.getRequestedCash() - action.getOfferedCash());
                    receiver.changeBalance(action.getOfferedCash() - action.getRequestedCash());

                    // Swap properties back to original owners
                    for (Property p : action.getOfferedProperties()) {
                        receiver.removeProperty(p.getPropertyId());
                        sender.addProperty(p);
                    }
                    for (Property p : action.getRequestedProperties()) {
                        sender.removeProperty(p.getPropertyId());
                        receiver.addProperty(p);
                    }
                    gameState.addEvent("UNDO: Trade reverted.");
                }
                break;

            default:
                gameState.addEvent("UNDO: Action type not supported.");
        }
    }

    /**
     * Re-applies the last undone action.
     */
    public void redo() {
        if (redoStack.isEmpty()) {
            gameState.addEvent("REDO: No action to redo.");
            return;
        }

        GameAction action = redoStack.pop();
        undoStack.push(action);

        switch (action.getType()) {
            case MONEY_CHANGE:
                int diff = (Integer) action.getNewValue() - (Integer) action.getOldValue();
                Player pMoney = gameState.getPlayerById(action.getPlayerId());
                if (pMoney != null) {
                    pMoney.changeBalance(diff);
                }
                gameState.addEvent("REDO: Money change re-applied.");
                break;

            case MOVEMENT:
                Player pMove = gameState.getPlayerById(action.getPlayerId());
                if (pMove != null) {
                    pMove.setCurrentPosition((Integer) action.getNewValue());
                }
                gameState.addEvent("REDO: Movement re-applied.");
                break;

            case PROPERTY_PURCHASE:
                Player pPurchase = gameState.getPlayerById(action.getPlayerId());
                Property prop = gameState.getPropertyById(action.getTargetId());
                if (pPurchase != null && prop != null) {
                    pPurchase.changeBalance(-prop.getPurchasePrice());
                    pPurchase.addProperty(prop);
                }
                gameState.addEvent("REDO: Property purchase re-applied.");
                break;

            case CONSTRUCTION:
                Player pBuild = gameState.getPlayerById(action.getPlayerId());
                Property builtProp = gameState.getPropertyById(action.getTargetId());
                if (pBuild != null && builtProp != null) {
                    pBuild.changeBalance(-builtProp.getHouseCost());
                    if (builtProp.getHouseCount() == 4) {
                        builtProp.addHotel();
                    } else {
                        builtProp.addHouse();
                    }
                }
                gameState.addEvent("REDO: Construction re-applied.");
                break;

            default:
                gameState.addEvent("REDO: Action re-applied.");
        }
    }
}
