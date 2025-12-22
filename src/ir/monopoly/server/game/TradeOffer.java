package ir.monopoly.server.game;

import ir.monopoly.server.property.Property;

public class TradeOffer {
    private final int fromPlayerId;
    private final int toPlayerId;
    private final int offeredCash;
    private final int requestedCash;
    private final Property[] offeredProperties;
    private final Property[] requestedProperties;

    public TradeOffer(int from, int to, int offCash, int reqCash, Property[] offProps, Property[] reqProps) {
        this.fromPlayerId = from;
        this.toPlayerId = to;
        this.offeredCash = offCash;
        this.requestedCash = reqCash;
        this.offeredProperties = offProps;
        this.requestedProperties = reqProps;
    }

    public int getFromPlayerId() {
        return fromPlayerId;
    }

    public int getToPlayerId() {
        return toPlayerId;
    }

    public int getOfferedCash() {
        return offeredCash;
    }

    public int getRequestedCash() {
        return requestedCash;
    }

    public Property[] getOfferedProperties() {
        return offeredProperties;
    }

    public Property[] getRequestedProperties() {
        return requestedProperties;
    }
}
