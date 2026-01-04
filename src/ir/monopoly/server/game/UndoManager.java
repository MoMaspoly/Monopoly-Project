package ir.monopoly.server.game;

import ir.monopoly.server.datastructure.MyStack;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;

public class UndoManager {
    private final MyStack<GameAction> undoStack;
    private final MyStack<GameAction> redoStack;
    private final GameState gameState;

    public UndoManager(GameState gameState) {
        this.gameState = gameState;
        this.undoStack = new MyStack<>();
        this.redoStack = new MyStack<>();
    }

    public void recordAction(GameAction action) {
        undoStack.push(action);
        while (!redoStack.isEmpty()) {
            redoStack.pop();
        }
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            gameState.addEvent("UNDO: No action to undo.");
            return;
        }

        GameAction action = undoStack.pop();
        redoStack.push(action);

        switch (action.getType()) {
            case MONEY_CHANGE:
                int diff = (Integer) action.getNewValue() - (Integer) action.getOldValue();
                Player playerMoney = gameState.getPlayerById(action.getPlayerId());
                if (playerMoney != null) {
                    playerMoney.changeBalance(-diff);
                    gameState.updatePlayerRankings(playerMoney);
                }
                gameState.addEvent("UNDO: Money change reverted by $" + diff);
                break;

            case PROPERTY_PURCHASE:
                Player playerPurchase = gameState.getPlayerById(action.getPlayerId());
                Property purchasedProp = gameState.getPropertyById(action.getTargetId());
                if (playerPurchase != null && purchasedProp != null) {
                    playerPurchase.changeBalance(purchasedProp.getPurchasePrice());
                    playerPurchase.removeProperty(purchasedProp.getPropertyId());
                    purchasedProp.clearOwner();
                    gameState.updatePlayerRankings(playerPurchase);
                    gameState.addEvent("UNDO: Property purchase reverted for " + purchasedProp.getName());
                }
                break;

            case CONSTRUCTION:
                Player playerBuild = gameState.getPlayerById(action.getPlayerId());
                Property builtProp = gameState.getPropertyById(action.getTargetId());
                if (playerBuild != null && builtProp != null) {
                    playerBuild.changeBalance((Integer) action.getOldValue());
                    if (builtProp.hasHotel()) {
                        builtProp.removeHotel();
                    } else {
                        builtProp.removeHouses((Integer) action.getNewValue());
                    }
                    gameState.updatePlayerRankings(playerBuild);
                    gameState.addEvent("UNDO: Construction reverted on " + builtProp.getName());
                }
                break;

            case MORTGAGE:
                Player playerMortgage = gameState.getPlayerById(action.getPlayerId());
                Property mortgagedProp = gameState.getPropertyById(action.getTargetId());
                if (playerMortgage != null && mortgagedProp != null) {
                    boolean wasMortgaged = (Boolean) action.getOldValue();
                    if (wasMortgaged) {
                        playerMortgage.changeBalance(-mortgagedProp.getUnmortgagePrice());
                    } else {
                        playerMortgage.changeBalance(mortgagedProp.getMortgageValue());
                    }
                    mortgagedProp.setMortgaged(!wasMortgaged);
                    gameState.updatePlayerRankings(playerMortgage);
                    gameState.addEvent("UNDO: Mortgage status reverted for property " + mortgagedProp.getName());
                }
                break;

            case MOVEMENT:
                Player playerMove = gameState.getPlayerById(action.getPlayerId());
                if (playerMove != null) {
                    playerMove.setCurrentPosition((Integer) action.getOldValue());
                    gameState.addEvent("UNDO: Movement reverted to position " + action.getOldValue());
                }
                break;

            case TRADE:
                Player sender = gameState.getPlayerById(action.getPlayerId());
                Player receiver = gameState.getPlayerById(action.getOtherPlayerId());

                if (sender == null || receiver == null) {
                    gameState.addEvent("UNDO TRADE: One of the players not found.");
                    break;
                }

                sender.changeBalance(action.getRequestedCash());
                receiver.changeBalance(action.getOfferedCash());

                for (Property p : action.getOfferedProperties()) {
                    if (p != null) {
                        receiver.removeProperty(p.getPropertyId());
                        sender.addProperty(p);
                        p.setOwner(sender.getPlayerId());
                    }
                }

                for (Property p : action.getRequestedProperties()) {
                    if (p != null) {
                        sender.removeProperty(p.getPropertyId());
                        receiver.addProperty(p);
                        p.setOwner(receiver.getPlayerId());
                    }
                }

                gameState.updatePlayerRankings(sender);
                gameState.updatePlayerRankings(receiver);
                gameState.addEvent("UNDO: Trade between " + sender.getName() + " and " + receiver.getName() + " reverted.");
                break;

            default:
                gameState.addEvent("UNDO: Action type not supported: " + action.getType());
        }
    }

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
                Player playerMoney = gameState.getPlayerById(action.getPlayerId());
                if (playerMoney != null) {
                    playerMoney.changeBalance(diff);
                    gameState.updatePlayerRankings(playerMoney);
                }
                gameState.addEvent("REDO: Money change re-applied: $" + diff);
                break;

            case MOVEMENT:
                Player playerMove = gameState.getPlayerById(action.getPlayerId());
                if (playerMove != null) {
                    playerMove.setCurrentPosition((Integer) action.getNewValue());
                    gameState.addEvent("REDO: Movement re-applied to position " + action.getNewValue());
                }
                break;

            case PROPERTY_PURCHASE:
                // خرید مجدد ملک
                Player playerPurchase = gameState.getPlayerById(action.getPlayerId());
                Property purchasedProp = gameState.getPropertyById(action.getTargetId());
                if (playerPurchase != null && purchasedProp != null) {
                    playerPurchase.changeBalance(-purchasedProp.getPurchasePrice());
                    playerPurchase.addProperty(purchasedProp);
                    purchasedProp.setOwner(playerPurchase.getPlayerId());
                    gameState.updatePlayerRankings(playerPurchase);
                    gameState.addEvent("REDO: Property purchase re-applied for " + purchasedProp.getName());
                }
                break;

            case CONSTRUCTION:
                Player playerBuild = gameState.getPlayerById(action.getPlayerId());
                Property builtProp = gameState.getPropertyById(action.getTargetId());
                if (playerBuild != null && builtProp != null) {
                    playerBuild.changeBalance(-(Integer) action.getOldValue()); // کسر هزینه خانه
                    if (action.getNewValue().equals(5)) { // اگر به هتل رسیده بود
                        builtProp.addHotel();
                    } else {
                        builtProp.addHouse();
                    }
                    gameState.updatePlayerRankings(playerBuild);
                    gameState.addEvent("REDO: Construction re-applied on " + builtProp.getName());
                }
                break;

            default:
                gameState.addEvent("REDO: Action " + action.getType() + " re-applied.");
        }
    }
}