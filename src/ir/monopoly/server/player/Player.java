package ir.monopoly.server.player;

import ir.monopoly.server.datastructure.MyStack;
import ir.monopoly.server.datastructure.PropertyTree;
import ir.monopoly.server.property.Property;

public class Player implements Comparable<Player> {
    private final int playerId;
    private final String name;
    private int balance;
    private int currentPosition;
    private PlayerStatus status;
    private int jailTurns = 0;
    private boolean hasChanceJailCard = false;
    private boolean hasCommunityJailCard = false;
    private PropertyTree ownedProperties;

    public Player(int playerId, String name, int initialBalance) {
        this.playerId = playerId;
        this.name = name;
        this.balance = initialBalance;
        this.currentPosition = 0;
        this.status = PlayerStatus.ACTIVE;
        this.ownedProperties = new PropertyTree();
    }

    public void incrementJailTurns() { this.jailTurns++; }
    public int getJailTurns() { return jailTurns; }
    public void resetJailTurns() { this.jailTurns = 0; }

    public void releaseFromJail() {
        this.status = PlayerStatus.ACTIVE;
        this.resetJailTurns();
    }

    public void addGetOutOfJailFreeCard(boolean isChance) {
        if (isChance) hasChanceJailCard = true;
        else hasCommunityJailCard = true;
    }

    public boolean useJailCard() {
        if (hasChanceJailCard) {
            hasChanceJailCard = false;
            return true;
        } else if (hasCommunityJailCard) {
            hasCommunityJailCard = false;
            return true;
        }
        return false;
    }

    public int getPlayerId() { return playerId; }
    public String getName() { return name; }
    public int getBalance() { return balance; }
    public void changeBalance(int amount) { this.balance += amount; }
    public int getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(int position) { this.currentPosition = position; }
    public PlayerStatus getStatus() { return status; }
    public void setStatus(PlayerStatus status) { this.status = status; }
    public PropertyTree getOwnedProperties() { return ownedProperties; }

    public void addProperty(Property property) {
        ownedProperties.insert(property);
        property.setOwner(playerId);
    }

    public void removeProperty(int propertyId) {
        ownedProperties.remove(propertyId);
    }

    public int getTotalWealth() {
        final int[] sum = {0};
        ownedProperties.forEach(p -> {
            sum[0] += p.getPurchasePrice() + (p.getHouseCount() * p.getHouseCost());
            if (p.hasHotel()) sum[0] += p.getHouseCost();
        });
        return this.balance + sum[0];
    }

    public String getPropertiesString() {
        StringBuilder sb = new StringBuilder();
        ownedProperties.forEach(p -> {
            if (sb.length() > 0) sb.append("|");
            sb.append(p.getName())
                    .append(",").append(p.getColorGroup())
                    .append(",").append(p.getHouseCount())
                    .append(",").append(p.hasHotel())
                    .append(",").append(p.isMortgaged())
                    .append(",").append(p.getPurchasePrice())
                    .append(",").append(p.getPropertyId());
        });
        return sb.toString();
    }

    @Override
    public int compareTo(Player other) {
        return Integer.compare(this.getTotalWealth(), other.getTotalWealth());
    }
}