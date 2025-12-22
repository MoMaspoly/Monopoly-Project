package ir.monopoly.server.game;

import ir.monopoly.server.property.Property;

public class GameAction {
    public enum ActionType {
        MONEY_CHANGE, PROPERTY_PURCHASE, CONSTRUCTION, MORTGAGE, MOVEMENT, TRADE
    }

    private final ActionType type;
    private final int playerId;
    private final int otherPlayerId;
    private final Object oldValue;
    private final Object newValue;
    private final int targetId;
    private final Property[] offeredProperties;
    private final Property[] requestedProperties;
    private final int offeredCash;
    private final int requestedCash;

    public GameAction(ActionType type, int playerId, Object oldValue, Object newValue, int targetId) {
        this(type, playerId, -1, oldValue, newValue, targetId, null, null, 0, 0);
    }

    public GameAction(ActionType type, int senderId, int receiverId,
                      Property[] offeredProps, Property[] requestedProps,
                      int offeredCash, int requestedCash) {
        this(type, senderId, receiverId, null, null, -1, offeredProps, requestedProps, offeredCash, requestedCash);
    }

    private GameAction(ActionType type, int playerId, int otherPlayerId,
                       Object oldValue, Object newValue, int targetId,
                       Property[] offeredProperties, Property[] requestedProperties,
                       int offeredCash, int requestedCash) {
        this.type = type;
        this.playerId = playerId;
        this.otherPlayerId = otherPlayerId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.targetId = targetId;
        this.offeredProperties = offeredProperties;
        this.requestedProperties = requestedProperties;
        this.offeredCash = offeredCash;
        this.requestedCash = requestedCash;
    }

    public ActionType getType() { return type; }
    public int getPlayerId() { return playerId; }
    public int getOtherPlayerId() { return otherPlayerId; }
    public Object getOldValue() { return oldValue; }
    public Object getNewValue() { return newValue; }
    public int getTargetId() { return targetId; }
    public Property[] getOfferedProperties() { return offeredProperties; }
    public Property[] getRequestedProperties() { return requestedProperties; }
    public int getOfferedCash() { return offeredCash; }
    public int getRequestedCash() { return requestedCash; }
}